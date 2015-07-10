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
   * NOTE: Please do not use this method for performance sensitive apps, but use the streaming
   * API instead.  ByteArrayOutputStream is known to be very slow.  Anyways if you care about
   * lots of data you would be streaming, right?  right?  :)
   *
   * @param item the item to serialize
   * @param initSize the default initial size of the ByteArray buffer.  If you write
   *        a large object this must be raised or else your app will spend lots of time
   *        growing the ByteArray.
   * @return the packed data
   * @throws UnpackableItemException If the given data cannot be packed.
   */
  def pack[A: Codec](item: A, initSize: Int = 512): Array[Byte] = {
    val out = new ByteArrayOutputStream(initSize)
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