package org.velvia

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.nio.ByteBuffer


class InvalidMsgPackDataException(msg: String) extends Exception(msg)

/**
 * A super simple MessagePack serialization library for Scala
 * based on the msgpack-java-lite project
 * https://bitbucket.org/sirbrialliance
 *
 * This implementation uses the builtin Scala types:
 * - null
 * - Boolean
 * - Number
 *     byte, short, int, and long will be packed and unpacked into the smallest possible representation,
 *     BigInt's will be used for anything larger than long.MAX_VALUE
 * - String (UTF-8), Array[Byte], or ByteBuffer (the *whole* buffer) (defaults to unpacked as a String)
 * - Map (any Map may be used for packing, always unpacked as a HashMap)
 * - Seq (any Seq may be used for packing, always unpacked as a Vector)
 * - Arrays (always unpacked as a Vector)
 * Passing any other types will throw an IllegalArumentException.
 *
 * @author velvia
 */
object MsgPack extends PackingUtils {
  import org.velvia.msgpack.Format._

  /**
   * Packs an item using the msgpack protocol.
   *
   * Warning: this does not do any recursion checks. If you pass a cyclic object,
   * you will run in an infinite loop until you run out of memory.
   *
   * @param item
   * @return the packed data
   * @throws UnpackableItemException If the given data cannot be packed.
   */
  def pack(item: Any): Array[Byte] = {
    val out = new ByteArrayOutputStream();
    try {
      pack(item, new DataOutputStream(out));
    } catch {
      case e: IOException =>
        //this shouldn't happen
        throw new RuntimeException("ByteArrayOutputStream threw an IOException!", e);
    }
    out.toByteArray();
  }

  /**
   * Unpacks the given data.
   *
   * @param data the byte array to unpack
   * @return the unpacked data
   * @throws InvalidMsgPackDataException If the given data cannot be unpacked.
   */
  def unpack(data: Array[Byte]): Any = {
    val in = new ByteArrayInputStream(data)
    try {
      unpack(new DataInputStream(in))
    } catch {
      case ex: InvalidMsgPackDataException =>  throw ex
      case ex: IOException =>            //this shouldn't happen
        throw new RuntimeException("ByteArrayInStream threw an IOException!", ex);
    }
  }

  /**
   * Packs the item, streaming the data to the given OutputStream.
   * Warning: this does not do any recursion checks. If you pass a cyclic object,
   * you will run in an infinite loop until you run out of memory/space to write.
   * @param item
   * @param out
   */
  def pack(item: Any, out: DataOutputStream) {
    item match {
      case s: String => packString(s, out)
      case n: Number => n.asInstanceOf[Any] match {
          case f: Float  =>  out.write(MP_FLOAT); out.writeFloat(f)
          case d: Double => out.write(MP_DOUBLE); out.writeDouble(d)
          case _         => packLong(n.longValue, out)
        }
      case map: collection.Map[Any, Any] => packMap(map, out)
      case s: Seq[_]      => packSeq(s, out)
      case b: Array[Byte] => packRawBytes(b, out)
      case a: Array[_]    => packArray(a, out)
      case bb: ByteBuffer =>
        if (bb.hasArray())
          packRawBytes(bb.array, out)
        else {
          val data = new Array[Byte](bb.capacity())
          bb.position(); bb.limit(bb.capacity())
          bb.get(data)
          packRawBytes(data, out)
        }
      case x: Boolean =>  out.write(if (x) MP_TRUE else MP_FALSE)
      case null       =>  out.write(MP_NULL)
      case item =>
        throw new IllegalArgumentException("Cannot msgpack object of type " + item.getClass().getCanonicalName());
    }
  }

  /**
   * Unpacks the item, streaming the data from the given OutputStream.
   * @param in Input stream to read from
   * @param compatibilityMode True for compatibility mode
   *          MessagePack format used to pack raw bytes and strings with the same message format
   *          header (0xda/b, 0xa0-0xbf). If you want compatibility with old MessagePack messages,
   *          setting this to True will return all String formatted messages as raw bytes instead.
   *          The old UNPACK_RAW_AS_STRING option becomes the default behavior now since the old
   *          RAW format is now the STRING format.
   * @throws IOException if the underlying stream has an error
   * @throws InvalidMsgPackDataException If the given data cannot be unpacked.
   */
  def unpack(in: DataInputStream, compatibilityMode: Boolean = false): Any = {
    val value = in.read()
    val compatModeOption = if (compatibilityMode) 0 else UNPACK_RAW_AS_STRING
    if (value < 0) throw new InvalidMsgPackDataException("No more input available when expecting a value")

    try {
      // Ordering of statements has a large effect on unpacking time
      // The current ordering is optimized for short maps, lists, strings, and small numbers
      value.toByte match {
        case b if b >= MP_NEGATIVE_FIXNUM => value.toByte     // All numbers between -32 and +127
        case b if (b >= MP_FIXSTR && b <= MP_FIXSTR + MAX_5BIT) =>
          unpackRaw(value - MP_FIXSTR_INT, in, compatModeOption)
        case b if (b >= MP_FIXARRAY && b <= MP_FIXARRAY + MAX_4BIT) =>
          unpackSeq(value - MP_FIXARRAY_INT, in, compatibilityMode)
        case b if (b >= MP_FIXMAP && b <= MP_FIXMAP + MAX_4BIT) =>
          unpackMap(value - MP_FIXMAP_INT, in, compatibilityMode)
        // Compatibility mode not needed for STR8, which did not previously exist
        case MP_STR8  => unpackRaw(in.read(), in, UNPACK_RAW_AS_STRING)
        case MP_UINT16 => in.readShort() & MAX_16BIT //read short, trick Java into treating it as unsigned, return int
        case MP_UINT32 => in.readInt() & MAX_32BIT //read int, trick Java into treating it as unsigned, return long
        case MP_INT16 => in.readShort()
        case MP_INT32 => in.readInt()
        case MP_UINT64 =>
          val v = in.readLong()
          if (v >= 0) v
          else
            //this is a little bit more tricky, since we don't have unsigned longs
            math.BigInt(1, Array[Byte](((v >> 24) & 0xff).toByte,
                                       ((v >> 16) & 0xff).toByte,
                                       ((v >> 8) & 0xff).toByte, (v & 0xff).toByte))
        case MP_UINT8 =>  in.read()       //read single byte, return as int
        case MP_INT8 =>  in.read().toByte
        case MP_INT64 => in.readLong()
        case MP_STR16 => unpackRaw(in.readShort() & MAX_16BIT, in, compatModeOption)
        case MP_STR32 => unpackRaw(in.readInt(), in, compatModeOption)
        case MP_ARRAY16 => unpackSeq(in.readShort() & MAX_16BIT, in, compatibilityMode)
        case MP_ARRAY32 => unpackSeq(in.readInt(), in, compatibilityMode);
        case MP_MAP16 => unpackMap(in.readShort() & MAX_16BIT, in, compatibilityMode)
        case MP_MAP32 => unpackMap(in.readInt(), in, compatibilityMode);
        case MP_RAW8  => unpackRaw(in.read(), in, 0)
        case MP_RAW16 => unpackRaw(in.readShort() & MAX_16BIT, in, 0)
        case MP_RAW32 => unpackRaw(in.readInt(), in, 0)
        case MP_FALSE =>  false
        case MP_TRUE =>   true
        case MP_FLOAT =>  in.readFloat()
        case MP_DOUBLE => in.readDouble()
        case MP_NULL =>   null
        case _ =>
          throw new InvalidMsgPackDataException("Input contains invalid type value")
      }

    } catch {
      case ex: EOFException =>
        throw new InvalidMsgPackDataException("No more input available when expecting a value");
    }
  }
}