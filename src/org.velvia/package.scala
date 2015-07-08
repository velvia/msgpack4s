package org.velvia

import java.io.{DataInputStream, DataOutputStream, ByteArrayInputStream, ByteArrayOutputStream}
import java.io.IOException
import org.velvia.msgpack.Codec

/**
 * Contains newer typeclass-based APIs
 */
package object msgpack {
  def pack[A: Codec](item: A, out: DataOutputStream) { implicitly[Codec[A]].pack(out, item) }
  def unpack[A: Codec](in: DataInputStream): A = { implicitly[Codec[A]].unpack(in) }

  /**
   * Packs an item using the msgpack protocol and codec typeclasses
   *
   * Warning: this does not do any recursion checks. If you pass a cyclic object,
   * you will run in an infinite loop until you run out of memory.
   *
   * @param item
   * @return the packed data
   * @throws UnpackableItemException If the given data cannot be packed.
   */
  def pack[A: Codec](item: A): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    try {
      pack(item, new DataOutputStream(out))
    } catch {
      case e: IOException =>
        //this shouldn't happen
        throw new RuntimeException("ByteArrayOutputStream threw an IOException!", e)
    }
    out.toByteArray()
  }

  /**
   * Unpacks the given data using a typeclass
   *
   * @param data the byte array to unpack
   * @return the unpacked data
   * @throws InvalidMsgPackDataException If the given data cannot be unpacked.
   */
  def unpack[A: Codec](data: Array[Byte]): A = {
    val in = new ByteArrayInputStream(data)
    try {
      unpack(new DataInputStream(in))
    } catch {
      case ex: InvalidMsgPackDataException =>  throw ex
      case ex: IOException =>            //this shouldn't happen
        throw new RuntimeException("ByteArrayInStream threw an IOException!", ex);
    }
  }

}