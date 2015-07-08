package org.velvia

import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

trait PackingUtils {
  import org.velvia.msgpack.Format._

  def pack(item: Any, out: DataOutputStream)
  def unpack(in: DataInputStream, compatibilityMode: Boolean): Any

  protected def packMap(map: collection.Map[Any, Any], out: DataOutputStream) {
    if (map.size <= MAX_4BIT) {
      out.write(map.size | MP_FIXMAP)
    } else if (map.size <= MAX_16BIT) {
      out.write(MP_MAP16)
      out.writeShort(map.size)
    } else {
      out.write(MP_MAP32)
      out.writeInt(map.size)
    }
    map foreach { case (k, v) => pack(k, out); pack(v, out) }
  }

  protected def packSeq(s: Seq[Any], out: DataOutputStream) {
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
  }

  protected def packArray[T](a: Array[T], out: DataOutputStream) {
    if (a.length <= MAX_4BIT) {
      out.write(a.length | MP_FIXARRAY)
    } else if (a.length <= MAX_16BIT) {
      out.write(MP_ARRAY16)
      out.writeShort(a.length)
    } else {
      out.write(MP_ARRAY32)
      out.writeInt(a.length)
    }
    var i = 0
    while (i < a.length) {
      pack(a(i), out)
      i += 1
    }
  }

  protected def unpackSeq(size: Int, in: DataInputStream, compatibilityMode: Boolean): Seq[_] = {
    if (size < 0)
      throw new InvalidMsgPackDataException("Array to unpack too large for Java (more than 2^31 elements)!")
    val vec = Vector.newBuilder[Any]
    var i = 0
    while (i < size) {
      vec += unpack(in, compatibilityMode)
      i += 1
    }
    vec.result
  }

  protected def unpackMap(size: Int, in: DataInputStream, compatibilityMode: Boolean): Map[_, _] = {
    if (size < 0)
      throw new InvalidMsgPackDataException("Map to unpack too large for Java (more than 2^31 elements)!")
    var map = Map.newBuilder[Any, Any]
    for { i <- 0 until size } {
      map += unpack(in, compatibilityMode) -> unpack(in, compatibilityMode)
    }
    map.result
  }
}
