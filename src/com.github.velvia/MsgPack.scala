package com.github.velvia

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
 *     byte, short, int, and long are considered interchangeable when
 *     packing/unpacking, BigInt's will be used for large values in uint64 values
 * - String (UTF-8), byte[], or ByteBuffer (the *whole* buffer) (always unpacked as a byte[] unless you ask for something else)
 * - Map (any type may be used for packing, always unpacked as a HashMap)
 * - Seq (any type may be used for packing, always unpacked as a Vector)
 * Passing any other types will throw an IllegalArumentException.
 *
 * @author velvia
 */
object MsgPack {
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

  val UNPACK_RAW_AS_STRING = 0x1
  val UNPACK_RAW_AS_BYTE_BUFFER = 0x2
  val DEFAULT_OPTIONS = UNPACK_RAW_AS_STRING

  /**
   * Unpacks the given data.
   *
   * @param packed data
   * @param int options, defaults to DEFAULT_OPTIONS
   * Bitmask of flags to specify how to map certain values back to java types:
   * For raw types:
   *    (no option) - All raw bytes are decoded as a byte[]
   *    OPTION_RAW_AS_STRING - All raw bytes are decoded as a UTF-8 string, with invalid codepoints replaced with a placeholder
   *    OPTION_RAW_AS_BYTE_BUFFER - All raw bytes are decoded as ByteBuffers (with a backing array)
   * @return the unpacked data
   * @throws InvalidMsgPackDataException If the given data cannot be unpacked.
   */
  def unpack(data: Array[Byte], options: Int = DEFAULT_OPTIONS): Any = {
    val in = new ByteArrayInputStream(data)
    try {
      unpack(new DataInputStream(in), options)
    } catch {
      case ex: InvalidMsgPackDataException =>  throw ex
      case ex: IOException =>            //this shouldn't happen
        throw new RuntimeException("ByteArrayInStream threw an IOException!", ex);
    }
  }

  protected val MAX_4BIT = 0xf
  protected val MAX_5BIT = 0x1f
  protected val MAX_7BIT = 0x7f
  protected val MAX_8BIT = 0xff
  protected val MAX_15BIT = 0x7fff
  protected val MAX_16BIT = 0xffff
  protected val MAX_31BIT = 0x7fffffff
  protected val MAX_32BIT = 0xffffffffL

  //these values are from http://wiki.msgpack.org/display/MSGPACK/Format+specification
  protected val MP_NULL = 0xc0.toByte;
  protected val MP_FALSE = 0xc2.toByte;
  protected val MP_TRUE = 0xc3.toByte;

  protected val MP_FLOAT = 0xca.toByte;
  protected val MP_DOUBLE = 0xcb.toByte;

  protected val MP_FIXNUM = 0x00.toByte;//last 7 bits is value
  protected val MP_UINT8 = 0xcc.toByte;
  protected val MP_UINT16 = 0xcd.toByte;
  protected val MP_UINT32 = 0xce.toByte;
  protected val MP_UINT64 = 0xcf.toByte;

  protected val MP_NEGATIVE_FIXNUM = 0xe0.toByte;//last 5 bits is value
  protected val MP_NEGATIVE_FIXNUM_INT = 0xe0;//  /me wishes for signed numbers.
  protected val MP_INT8 = 0xd0.toByte;
  protected val MP_INT16 = 0xd1.toByte;
  protected val MP_INT32 = 0xd2.toByte;
  protected val MP_INT64 = 0xd3.toByte;

  protected val MP_FIXARRAY = 0x90.toByte;//last 4 bits is size
  protected val MP_FIXARRAY_INT = 0x90;
  protected val MP_ARRAY16 = 0xdc.toByte;
  protected val MP_ARRAY32 = 0xdd.toByte;

  protected val MP_FIXMAP = 0x80.toByte;//last 4 bits is size
  protected val MP_FIXMAP_INT = 0x80;
  protected val MP_MAP16 = 0xde.toByte;
  protected val MP_MAP32 = 0xdf.toByte;

  protected val MP_FIXRAW = 0xa0.toByte;//last 5 bits is size
  protected val MP_FIXRAW_INT = 0xa0;
  protected val MP_RAW16 = 0xda.toByte;
  protected val MP_RAW32 = 0xdb.toByte;

  /**
   * Packs the item, streaming the data to the given OutputStream.
   * Warning: this does not do any recursion checks. If you pass a cyclic object,
   * you will run in an infinite loop until you run out of memory/space to write.
   * @param item
   * @param out
   */
  def pack(item: Any, out: DataOutputStream) {
    item match {
      case x: String =>      writeRawBytes(x.getBytes("UTF-8"), out)
      case n: Number  =>  n.asInstanceOf[Any] match {
          case f: Float =>  out.write(MP_FLOAT); out.writeFloat(f)
          case d: Double => out.write(MP_DOUBLE); out.writeDouble(d)
          case _ =>
            val value = n.longValue
            if (value >= 0) {
              if (value <= MAX_7BIT) {
                out.write(value.toInt | MP_FIXNUM)
              } else if (value <= MAX_8BIT) {
                out.write(MP_UINT8)
                out.write(value.toInt)
              } else if (value <= MAX_16BIT) {
                out.write(MP_UINT16);
                out.writeShort(value.toInt)
              } else if (value <= MAX_32BIT) {
                out.write(MP_UINT32)
                out.writeInt(value.toInt)
              } else {
                out.write(MP_UINT64)
                out.writeLong(value)
              }
            } else {
              if (value >= -(MAX_5BIT + 1)) {
                out.write((value & 0xff).toInt)
              } else if (value >= -(MAX_7BIT + 1)) {
                out.write(MP_INT8)
                out.write(value.toInt)
              } else if (value >= -(MAX_15BIT + 1)) {
                out.write(MP_INT16)
                out.writeShort(value.toInt)
              } else if (value >= -(MAX_31BIT + 1)) {
                out.write(MP_INT32)
                out.writeInt(value.toInt)
              } else {
                out.write(MP_INT64)
                out.writeLong(value)
              }
            }
        }
      case map: collection.Map[_, _] =>
        if (map.size <= MAX_4BIT) {
          out.write(map.size | MP_FIXMAP);
        } else if (map.size <= MAX_16BIT) {
          out.write(MP_MAP16);
          out.writeShort(map.size);
        } else {
          out.write(MP_MAP32);
          out.writeInt(map.size);
        }
        map foreach { case (k, v) => pack(k, out); pack(v, out) }
      case s: Seq[_] =>
        if (s.length <= MAX_4BIT) {
          out.write(s.length | MP_FIXARRAY)
        } else if (s.length <= MAX_16BIT) {
          out.write(MP_ARRAY16)
          out.writeShort(s.length)
        } else {
          out.write(MP_ARRAY32)
          out.writeInt(s.length)
        }
        s foreach { pack(_, out) }
      case b: Array[Byte] => writeRawBytes(b, out)
      case bb: ByteBuffer =>
        if (bb.hasArray())
          writeRawBytes(bb.array, out)
        else {
          val data = new Array[Byte](bb.capacity())
          bb.position(); bb.limit(bb.capacity())
          bb.get(data)
          writeRawBytes(data, out)
        }
      case x: Boolean =>  out.write(if (x) MP_TRUE else MP_FALSE)
      case null       =>  out.write(MP_NULL)
      case item =>
        throw new IllegalArgumentException("Cannot msgpack object of type " + item.getClass().getCanonicalName());
    }
  }

  private def writeRawBytes(data: Array[Byte], out: DataOutputStream) {
    if (data.length <= MAX_5BIT) {
      out.write(data.length | MP_FIXRAW)
    } else if (data.length <= MAX_16BIT) {
      out.write(MP_RAW16)
      out.writeShort(data.length)
    } else {
      out.write(MP_RAW32)
      out.writeInt(data.length)
    }
    out.write(data)
  }

  /**
   * Unpacks the item, streaming the data from the given OutputStream.
   * Warning: this does not do any recursion checks. If you pass a cyclic object,
   * you will run in an infinite loop until you run out of memory/space to write.
   * @param in Input stream to read from
   * @param options Bitmask of options, @see unpack(byte[] data, int options)
   * @throws IOException if the underlying stream has an error
   * @throws InvalidMsgPackDataException If the given data cannot be unpacked.
   */
  def unpack(in: DataInputStream, options: Int): Any = {
    val value = in.read()
    if (value < 0) throw new InvalidMsgPackDataException("No more input available when expecting a value")

    try {
      var size = 0
      value.toByte match {
        case MP_NULL =>   null
        case MP_FALSE =>  false
        case MP_TRUE =>   true
        case MP_FLOAT =>  in.readFloat()
        case MP_DOUBLE => in.readDouble()
        case MP_UINT8 =>  in.read()       //read single byte, return as int
        case MP_UINT16 => in.readShort() & MAX_16BIT //read short, trick Java into treating it as unsigned, return int
        case MP_UINT32 => in.readInt() & MAX_32BIT //read int, trick Java into treating it as unsigned, return long
        case MP_UINT64 =>
          val v = in.readLong()
          if (v >= 0) v
          else
            //this is a little bit more tricky, since we don't have unsigned longs
            math.BigInt(1, Array[Byte](((v >> 24) & 0xff).toByte,
                                       ((v >> 16) & 0xff).toByte,
                                       ((v >> 8) & 0xff).toByte, (v & 0xff).toByte))
        case MP_INT8 =>  in.read().toByte
        case MP_INT16 => in.readShort()
        case MP_INT32 => in.readInt()
        case MP_INT64 => in.readLong()
        case MP_ARRAY16 => unpackList(in.readShort() & MAX_16BIT, in, options)
        case MP_ARRAY32 => unpackList(in.readInt(), in, options);
        case MP_MAP16 => unpackMap(in.readShort() & MAX_16BIT, in, options)
        case MP_MAP32 => unpackMap(in.readInt(), in, options);
        case MP_RAW16 => unpackRaw(in.readShort() & MAX_16BIT, in, options)
        case MP_RAW32 => unpackRaw(in.readInt(), in, options)
        case _ =>
          if (value >= MP_NEGATIVE_FIXNUM_INT && value <= MP_NEGATIVE_FIXNUM_INT + MAX_5BIT) {
            value.toByte
          } else if (value >= MP_FIXARRAY_INT && value <= MP_FIXARRAY_INT + MAX_4BIT) {
            unpackList(value - MP_FIXARRAY_INT, in, options)
          } else if (value >= MP_FIXMAP_INT && value <= MP_FIXMAP_INT + MAX_4BIT) {
            unpackMap(value - MP_FIXMAP_INT, in, options)
          } else if (value >= MP_FIXRAW_INT && value <= MP_FIXRAW_INT + MAX_5BIT) {
            unpackRaw(value - MP_FIXRAW_INT, in, options)
          } else if (value <= MAX_7BIT) { //MP_FIXNUM - the value is value as an int
            value
          } else throw new InvalidMsgPackDataException("Input contains invalid type value")
      }

    } catch {
      case ex: EOFException =>
        throw new InvalidMsgPackDataException("No more input available when expecting a value");
    }
  }

  def unpackList(size: Int, in: DataInputStream, options: Int): Seq[_] = {
    if (size < 0)
      throw new InvalidMsgPackDataException("Array to unpack too large for Java (more than 2^31 elements)!")
    Vector((0 until size).map { x => unpack(in, options) } :_*)
  }

  def unpackMap(size: Int, in: DataInputStream, options: Int): Map[_, _] = {
    if (size < 0)
      throw new InvalidMsgPackDataException("Map to unpack too large for Java (more than 2^31 elements)!")
    var map = collection.immutable.HashMap.empty[Any, Any]
    (0 until size).foreach { n => map = map + (unpack(in, options) -> unpack(in, options)) }
    map
  }

  def unpackRaw(size: Int, in: DataInputStream, options: Int): Any = {
    if (size < 0)
      throw new InvalidMsgPackDataException("byte[] to unpack too large for Java (more than 2^31 elements)!")

    val data = new Array[Byte](size)
    in.read(data)

    if ((options & UNPACK_RAW_AS_BYTE_BUFFER) != 0) {
      ByteBuffer.wrap(data)
    } else if ((options & UNPACK_RAW_AS_STRING) != 0) {
      new String(data, "UTF-8")
    } else data
  }
}