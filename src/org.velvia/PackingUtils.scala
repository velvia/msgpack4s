package org.velvia

import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

trait PackingUtils {
  val UNPACK_RAW_AS_STRING = 0x1
  val UNPACK_RAW_AS_BYTE_BUFFER = 0x2

  protected val MAX_4BIT = 0xf
  protected val MAX_5BIT = 0x1f
  protected val MAX_7BIT = 0x7f
  protected val MAX_8BIT = 0xff
  protected val MAX_15BIT = 0x7fff
  protected val MAX_16BIT = 0xffff
  protected val MAX_31BIT = 0x7fffffff
  protected val MAX_32BIT = 0xffffffffL

  //these values are from https://github.com/msgpack/msgpack/blob/master/spec.md
  protected val MP_NULL = 0xc0.toByte
  protected val MP_FALSE = 0xc2.toByte
  protected val MP_TRUE = 0xc3.toByte

  protected val MP_FLOAT = 0xca.toByte
  protected val MP_DOUBLE = 0xcb.toByte

  protected val MP_FIXNUM = 0x00.toByte //last 7 bits is value
  protected val MP_UINT8 = 0xcc.toByte
  protected val MP_UINT16 = 0xcd.toByte
  protected val MP_UINT32 = 0xce.toByte
  protected val MP_UINT64 = 0xcf.toByte

  protected val MP_NEGATIVE_FIXNUM = 0xe0.toByte //last 5 bits is value
  protected val MP_NEGATIVE_FIXNUM_INT = 0xe0 // /me wishes for signed numbers.
  protected val MP_INT8 = 0xd0.toByte
  protected val MP_INT16 = 0xd1.toByte
  protected val MP_INT32 = 0xd2.toByte
  protected val MP_INT64 = 0xd3.toByte

  protected val MP_FIXARRAY = 0x90.toByte //last 4 bits is size
  protected val MP_FIXARRAY_INT = 0x90
  protected val MP_ARRAY16 = 0xdc.toByte
  protected val MP_ARRAY32 = 0xdd.toByte

  protected val MP_FIXMAP = 0x80.toByte //last 4 bits is size
  protected val MP_FIXMAP_INT = 0x80
  protected val MP_MAP16 = 0xde.toByte
  protected val MP_MAP32 = 0xdf.toByte

  protected val MP_FIXSTR = 0xa0.toByte //last 5 bits is size
  protected val MP_FIXSTR_INT = 0xa0
  protected val MP_STR8  = 0xd9.toByte
  protected val MP_STR16 = 0xda.toByte
  protected val MP_STR32 = 0xdb.toByte

  // Raw bytes
  protected val MP_RAW8 = 0xc4.toByte
  protected val MP_RAW16 = 0xc5.toByte
  protected val MP_RAW32 = 0xc6.toByte

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

  protected def packRawBytes(data: Array[Byte], out: DataOutputStream) {
    if (data.length <= MAX_8BIT) {
      out.write(MP_RAW8)
      out.write(data.length)
    } else if (data.length <= MAX_16BIT) {
      out.write(MP_RAW16)
      out.writeShort(data.length)
    } else {
      out.write(MP_RAW32)
      out.writeInt(data.length)
    }
    out.write(data)
  }

  protected def packString(str: String, out: DataOutputStream) {
    if (str.length <= MAX_5BIT) {
      out.write(str.length | MP_FIXSTR)
    } else if (str.length <= MAX_8BIT) {
      out.write(MP_STR8)
      out.write(str.length)
    } else if (str.length <= MAX_16BIT) {
      out.write(MP_STR16)
      out.writeShort(str.length)
    } else {
      out.write(MP_STR32)
      out.writeInt(str.length)
    }
    out.write(str.getBytes("UTF-8"))
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
    var map = collection.immutable.HashMap.empty[Any, Any]
    var i = 0
    while (i < size) {
      map = map.updated(unpack(in, compatibilityMode), unpack(in, compatibilityMode))
      i += 1
    }
    map
  }

  // NOTE: MessagePack format used to pack raw bytes and strings with the same message format
  // header (0xda/b, 0xa0-0xbf).   If you want compatibility with old MessagePack messages,
  // you should call this with the appropriate flag.
  protected def unpackRaw(size: Int, in: DataInputStream, options: Int): Any = {
    if (size < 0)
      throw new InvalidMsgPackDataException("byte[] to unpack too large for Java (more than 2^31 elements)!")

    val data = new Array[Byte](size)
    // Don't use the standard read() method, it's not guaranteed to read back all the bytes!
    in.readFully(data)

    if ((options & UNPACK_RAW_AS_BYTE_BUFFER) != 0) {
      ByteBuffer.wrap(data)
    } else if ((options & UNPACK_RAW_AS_STRING) != 0) {
      new String(data, "UTF-8")
    } else data
  }
}
