package org.velvia

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException


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
 * NOTE:  This API is old.  Try the newer TypeClass-based API.
 *
 * @author velvia
 */
object MsgPack {
  import org.velvia.msgpack.Format._
  import org.velvia.msgpack.AnyCodecs._

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
    val out = new ByteArrayOutputStream()
    try {
      pack(item, new DataOutputStream(out))
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
    DefaultAnyCodec.pack(out, item)
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
    if (compatibilityMode) DefaultAnyCodecCompat.unpack(in)
    else                   DefaultAnyCodec.unpack(in)
  }
}