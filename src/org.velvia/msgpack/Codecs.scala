package org.velvia.msgpack

import java.io.{DataInputStream => DIS, DataOutputStream}
import org.velvia.InvalidMsgPackDataException

/**
 * A typeclass for packing and unpacking specific types from MessagePack
 */
trait Codec[@specialized(Int, Long, Short, Byte, Double, Float) A] {
  def pack(out: DataOutputStream, item: A)

  // unpack reads the next chunk from the DIS stream, including the first byte, and
  // throws an exception if the chunk does not match one of the supported types
  // in the unpackFuncMap.
  def unpack(in: DIS): A = {
    val headerByte = in.read().toByte
    val func = unpackFuncMap.getOrElse(headerByte, DefaultUnpackFunc )
    func(in)
  }

  type UnpackFunc = DIS => A

  val DefaultUnpackFunc: UnpackFunc =
    { dis => throw new InvalidMsgPackDataException("Unsupported header byte") }

  // The unpackFuncMap maps the first byte that contains the MessagePack format type
  // to a function passed a DIS to read any additional bytes required to unpack A.
  val unpackFuncMap: Map[Byte, UnpackFunc]
}

/**
 * Contains codecs for simple types: bool, ints, longs, floats, doubles
 */
object SimpleCodecs {
  import Format._

  implicit object BooleanCodec extends Codec[Boolean] {
    def pack(out: DataOutputStream, item: Boolean) {
      out.write(if (item) MP_TRUE else MP_FALSE)
    }

    val unpackFuncMap = Map[Byte, UnpackFunc](
      MP_FALSE -> { dis: DIS => false },
      MP_TRUE  -> { dis: DIS => true }
    )
  }

  val byteFuncMap: Map[Byte, DIS => Byte] = Range(MP_NEGATIVE_FIXNUM, Byte.MaxValue).map { i =>
    val b = i.toByte
    b -> { dis: DIS => b }
  }.toMap

  implicit object ByteCodec extends Codec[Byte] {
    def pack(out: DataOutputStream, item: Byte) { packLong(item.toLong, out) }
    val unpackFuncMap = Map[Byte, UnpackFunc](
      MP_UINT8 -> { in: DIS => in.read().toByte },
      MP_INT8 ->  { in: DIS => in.read().toByte }
    ) ++ byteFuncMap
  }

  implicit object ShortCodec extends Codec[Short] {
    def pack(out: DataOutputStream, item: Short) { packLong(item.toLong, out) }
    val unpackFuncMap = Map[Byte, UnpackFunc](
      MP_UINT16 -> { in: DIS => in.readShort() },
      MP_INT16  -> { in: DIS => in.readShort() },
      MP_UINT8  -> { in: DIS => in.read().toShort },
      MP_INT8   -> { in: DIS => in.read().toShort }
    ) ++ byteFuncMap.mapValues(_.andThen(_.toShort))
  }

  implicit object IntCodec extends Codec[Int] {
    def pack(out: DataOutputStream, item: Int) { packLong(item.toLong, out) }
    val unpackFuncMap = Map[Byte, UnpackFunc](
      MP_UINT16 -> { in: DIS => in.readShort() & MAX_16BIT },
      MP_INT16  -> { in: DIS => in.readShort().toInt },
      MP_UINT32 -> { in: DIS => in.readInt() },
      MP_INT32  -> { in: DIS => in.readInt() },
      MP_UINT8  -> { in: DIS => in.read().toInt },
      MP_INT8   -> { in: DIS => in.read().toInt }
    ) ++ byteFuncMap.mapValues(_.andThen(_.toInt))
  }

  implicit object LongCodec extends Codec[Long] {
    def pack(out: DataOutputStream, item: Long) { packLong(item, out) }
    val unpackFuncMap = Map[Byte, UnpackFunc](
      MP_UINT16 -> { in: DIS => (in.readShort() & MAX_16BIT).toLong },
      MP_INT16  -> { in: DIS => in.readShort().toLong },
      MP_UINT32 -> { in: DIS => in.readInt() & MAX_32BIT },
      MP_INT32  -> { in: DIS => in.readInt().toLong },
      MP_UINT64 -> { in: DIS => in.readLong() },
      MP_INT64  -> { in: DIS => in.readLong() },
      MP_UINT8  -> { in: DIS => in.read().toLong },
      MP_INT8   -> { in: DIS => in.read().toLong }
    ) ++ byteFuncMap.mapValues(_.andThen(_.toLong))
  }
}