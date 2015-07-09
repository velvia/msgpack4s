package org.velvia.msgpack

import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import org.velvia.InvalidMsgPackDataException

/**
 * Encapsulates the MessagePack binary format, according to
 * https://github.com/msgpack/msgpack/blob/master/spec.md
 */
object Format {
  val MAX_4BIT = 0xf
  val MAX_5BIT = 0x1f
  val MAX_7BIT = 0x7f
  val MAX_8BIT = 0xff
  val MAX_15BIT = 0x7fff
  val MAX_16BIT = 0xffff
  val MAX_31BIT = 0x7fffffff
  val MAX_32BIT = 0xffffffffL

  //these values are from https://github.com/msgpack/msgpack/blob/master/spec.md
  val MP_NULL = 0xc0.toByte
  val MP_FALSE = 0xc2.toByte
  val MP_TRUE = 0xc3.toByte

  val MP_FLOAT = 0xca.toByte
  val MP_DOUBLE = 0xcb.toByte

  val MP_FIXNUM = 0x00.toByte //last 7 bits is value
  val MP_UINT8 = 0xcc.toByte
  val MP_UINT16 = 0xcd.toByte
  val MP_UINT32 = 0xce.toByte
  val MP_UINT64 = 0xcf.toByte

  val MP_NEGATIVE_FIXNUM = 0xe0.toByte //last 5 bits is value
  val MP_NEGATIVE_FIXNUM_INT = 0xe0 // /me wishes for signed numbers.
  val MP_INT8 = 0xd0.toByte
  val MP_INT16 = 0xd1.toByte
  val MP_INT32 = 0xd2.toByte
  val MP_INT64 = 0xd3.toByte

  val MP_FIXARRAY = 0x90.toByte //last 4 bits is size
  val MP_FIXARRAY_INT = 0x90
  val MP_ARRAY16 = 0xdc.toByte
  val MP_ARRAY32 = 0xdd.toByte

  val MP_FIXMAP = 0x80.toByte //last 4 bits is size
  val MP_FIXMAP_INT = 0x80
  val MP_MAP16 = 0xde.toByte
  val MP_MAP32 = 0xdf.toByte

  val MP_FIXSTR = 0xa0.toByte //last 5 bits is size
  val MP_FIXSTR_INT = 0xa0
  val MP_STR8  = 0xd9.toByte
  val MP_STR16 = 0xda.toByte
  val MP_STR32 = 0xdb.toByte

  // Raw bytes
  val MP_RAW8 = 0xc4.toByte
  val MP_RAW16 = 0xc5.toByte
  val MP_RAW32 = 0xc6.toByte

  def packRawBytes(data: Array[Byte], out: DataOutputStream) {
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

  def packString(str: String, out: DataOutputStream) {
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

  def packLong(value: Long, out: DataOutputStream) {
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

  def packSeq[T: Codec](s: Seq[T], out: DataOutputStream) {
    val packer = implicitly[Codec[T]]
    if (s.length <= MAX_4BIT) {
      out.write(s.length | MP_FIXARRAY)
    } else if (s.length <= MAX_16BIT) {
      out.write(MP_ARRAY16)
      out.writeShort(s.length)
    } else {
      out.write(MP_ARRAY32)
      out.writeInt(s.length)
    }
    s foreach { packer.pack(out, _) }
  }

  def packMap[K: Codec, V: Codec](map: collection.Map[K, V], out: DataOutputStream) {
    val keyCodec = implicitly[Codec[K]]
    val valCodec = implicitly[Codec[V]]
    if (map.size <= MAX_4BIT) {
      out.write(map.size | MP_FIXMAP)
    } else if (map.size <= MAX_16BIT) {
      out.write(MP_MAP16)
      out.writeShort(map.size)
    } else {
      out.write(MP_MAP32)
      out.writeInt(map.size)
    }
    map foreach { case (k, v) => keyCodec.pack(out, k); valCodec.pack(out, v) }
  }

  val UNPACK_RAW_AS_STRING = 0x1
  val UNPACK_RAW_AS_BYTE_BUFFER = 0x2

  // NOTE: MessagePack format used to pack raw bytes and strings with the same message format
  // header (0xda/b, 0xa0-0xbf).   If you want compatibility with old MessagePack messages,
  // you should call this with the appropriate flag.
  def unpackRaw(size: Int, in: DataInputStream, options: Int): Any = {
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

  def unpackString(size: Int, in: DataInputStream): String =
    unpackRaw(size, in, UNPACK_RAW_AS_STRING).asInstanceOf[String]

  def unpackByteArray(size: Int, in: DataInputStream): Array[Byte] =
    unpackRaw(size, in, 0).asInstanceOf[Array[Byte]]

  def unpackSeq[T: Codec](size: Int, in: DataInputStream): Seq[T] = {
    val unpacker = implicitly[Codec[T]]
    if (size < 0)
      throw new InvalidMsgPackDataException("Array to unpack too large for Java (more than 2^31 elements)!")
    val vec = Vector.newBuilder[T]
    var i = 0
    while (i < size) {
      vec += unpacker.unpack(in)
      i += 1
    }
    vec.result
  }

  def unpackMap[K: Codec, V: Codec](size: Int, in: DataInputStream): Map[K, V] = {
    val keyCodec = implicitly[Codec[K]]
    val valCodec = implicitly[Codec[V]]
    if (size < 0)
      throw new InvalidMsgPackDataException("Map to unpack too large for Java (more than 2^31 elements)!")
    var map = Map.newBuilder[K, V]
    for { i <- 0 until size } {
      map += keyCodec.unpack(in) -> valCodec.unpack(in)
    }
    map.result
  }
}