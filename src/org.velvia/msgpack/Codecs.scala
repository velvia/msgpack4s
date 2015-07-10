package org.velvia.msgpack

import java.io.{DataInputStream => DIS, DataOutputStream, EOFException}
import java.nio.ByteBuffer
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
    try {
      val headerByte = in.readByte()
      val func = unpackFuncMap.getOrElse(headerByte, defaultUnpackFunc(headerByte) )
      func(in)
    } catch {
      case ex: EOFException =>
        throw new InvalidMsgPackDataException("No more input available when expecting a value")
    }
  }

  type UnpackFunc = DIS => A

  def defaultUnpackFunc(headerByte: Byte): UnpackFunc =
    { dis => throw new InvalidMsgPackDataException("Input contains invalid type value " + headerByte) }

  // The unpackFuncMap maps the first byte that contains the MessagePack format type
  // to a function passed a DIS to read any additional bytes required to unpack A.
  val unpackFuncMap: FastByteMap[UnpackFunc]
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

    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_FALSE -> { dis: DIS => false },
      MP_TRUE  -> { dis: DIS => true }
    )
  }

  val byteFuncMap = FastByteMap[DIS => Byte](
    (MP_NEGATIVE_FIXNUM.toInt to Byte.MaxValue).map(_.toByte).map { b =>
      b -> { dis: DIS => b }
    } :_*)

  implicit object ByteCodec extends Codec[Byte] {
    def pack(out: DataOutputStream, item: Byte) { packLong(item.toLong, out) }
    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_UINT8 -> { in: DIS => in.readByte() },
      MP_INT8 ->  { in: DIS => in.readByte() }
    ) ++ byteFuncMap
  }

  implicit object ShortCodec extends Codec[Short] {
    def pack(out: DataOutputStream, item: Short) { packLong(item.toLong, out) }
    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_UINT16 -> { in: DIS => in.readShort() },
      MP_INT16  -> { in: DIS => in.readShort() },
      MP_UINT8  -> { in: DIS => in.readUnsignedByte().toShort },
      MP_INT8   -> { in: DIS => in.readByte().toShort }
    ) ++ byteFuncMap.mapValues(_.andThen(_.toShort))
  }

  implicit object IntCodec extends Codec[Int] {
    def pack(out: DataOutputStream, item: Int) { packLong(item.toLong, out) }
    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_UINT16 -> { in: DIS => in.readShort() & MAX_16BIT },
      MP_INT16  -> { in: DIS => in.readShort().toInt },
      MP_UINT32 -> { in: DIS => in.readInt() },
      MP_INT32  -> { in: DIS => in.readInt() },
      MP_UINT8  -> { in: DIS => in.readUnsignedByte() },
      MP_INT8   -> { in: DIS => in.readByte().toInt }
    ) ++ byteFuncMap.mapValues(_.andThen(_.toInt))
  }

  implicit object LongCodec extends Codec[Long] {
    def pack(out: DataOutputStream, item: Long) { packLong(item, out) }
    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_UINT16 -> { in: DIS => (in.readShort() & MAX_16BIT).toLong },
      MP_INT16  -> { in: DIS => in.readShort().toLong },
      MP_UINT32 -> { in: DIS => in.readInt() & MAX_32BIT },
      MP_INT32  -> { in: DIS => in.readInt().toLong },
      MP_UINT64 -> { in: DIS => in.readLong() },
      MP_INT64  -> { in: DIS => in.readLong() },
      MP_UINT8  -> { in: DIS => in.readUnsignedByte().toLong },
      MP_INT8   -> { in: DIS => in.readByte().toLong }
    ) ++ byteFuncMap.mapValues(_.andThen(_.toLong))
  }

  implicit object FloatCodec extends Codec[Float] {
    def pack(out: DataOutputStream, item: Float) {
      out.write(MP_FLOAT)
      out.writeFloat(item)
    }

    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_FLOAT -> { in: DIS => in.readFloat() }
    )
  }

  implicit object DoubleCodec extends Codec[Double] {
    def pack(out: DataOutputStream, item: Double) {
      out.write(MP_DOUBLE)
      out.writeDouble(item)
    }

    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_DOUBLE -> { in: DIS => in.readDouble() }
    )
  }
}

/**
 * Codecs for newer MessagePack strings and raw bytes.
 * These codecs can also be used for older MessagePack messages if you did not encode raw byte values.
 */
object RawStringCodecs {
  import Format._

  implicit object StringCodec extends Codec[String] {
    def pack(out: DataOutputStream, item: String) { packString(item, out) }
    val unpackFuncMap = FastByteMap[UnpackFunc](
        MP_STR8  -> { in: DIS => unpackString(in.read(), in) },
        MP_STR16 -> { in: DIS => unpackString(in.readShort() & MAX_16BIT, in) },
        MP_STR32 -> { in: DIS => unpackString(in.readInt(), in) }
    ) ++ (0 to MAX_5BIT).map { strlen =>
      (MP_FIXSTR | strlen).toByte -> { in: DIS => unpackString(strlen, in) }
    }
  }

  implicit object ByteArrayCodec extends Codec[Array[Byte]] {
    def pack(out: DataOutputStream, item: Array[Byte]) { packRawBytes(item, out) }
    val unpackFuncMap = FastByteMap[UnpackFunc](
        MP_RAW8  -> { in: DIS => unpackByteArray(in.read(), in) },
        MP_RAW16 -> { in: DIS => unpackByteArray(in.readShort() & MAX_16BIT, in) },
        MP_RAW32 -> { in: DIS => unpackByteArray(in.readInt(), in) }
    )
  }

  val allFuncMaps = StringCodec.unpackFuncMap.mapAnyFunc ++
                    ByteArrayCodec.unpackFuncMap.mapAnyFunc
}

/**
 * Codecs to read older MessagePack messages as raw bytes instead of strings.
 * Note: it cannot be used to write.
 */
object CompatibilityCodecs {
  import Format._

  implicit object ByteArrayCodec extends Codec[Array[Byte]] {
    def pack(out: DataOutputStream, item: Array[Byte]) { ??? }
    val unpackFuncMap = FastByteMap[UnpackFunc](
        MP_STR16 -> { in: DIS => unpackByteArray(in.readShort() & MAX_16BIT, in) },
        MP_STR32 -> { in: DIS => unpackByteArray(in.readInt(), in) }
    ) ++ (0 to MAX_5BIT).map { strlen =>
      (MP_FIXSTR | strlen).toByte -> { in: DIS => unpackByteArray(strlen, in) }
    }
  }
}

/**
 * Codecs to read any value.
 * For Longs it is slightly different than the LongCodec because a UINT64 can return a BigInt
 * when the value is greater than Long.MaxValue
 */
object AnyCodecs {
  import Format._
  import CollectionCodecs._
  import SimpleCodecs._

  /**
   * A codec for packing and unpacking to and from Any values.
   * NOTE: it has to be defined like this so that we can create default objects which refer to
   * themselves as the base K and V codecs
   * @param compatibilityMode true if want to read raw bytes from old messagepack STR values
   */
  trait AnyCodecBase[K, V] extends Codec[Any] {
    def compatibilityMode: Boolean
    implicit def keyCodec: Codec[K]
    implicit def valCodec: Codec[V]
    val seqCodec = new SeqCodec[V]
    val mapCodec = new MapCodec[K, V]

    def pack(out: DataOutputStream, item: Any) {
      item match {
        case s: String => packString(s, out)
        case n: Number => n.asInstanceOf[Any] match {
            case f: Float  => FloatCodec.pack(out, f)
            case d: Double => DoubleCodec.pack(out, d)
            case _         => packLong(n.longValue, out)
          }
        case map: Map[K, V] => mapCodec.pack(out, map)
        case s: Seq[V]      => seqCodec.pack(out, s)
        case b: Array[Byte] => packRawBytes(b, out)
        case a: Array[V]    => seqCodec.pack(out, a.toSeq)
        case bb: ByteBuffer =>
          if (bb.hasArray())
            packRawBytes(bb.array, out)
          else {
            val data = new Array[Byte](bb.capacity())
            bb.position(); bb.limit(bb.capacity())
            bb.get(data)
            packRawBytes(data, out)
          }
        case x: Boolean => BooleanCodec.pack(out, x)
        case null       => out.write(MP_NULL)
        case item =>
          throw new IllegalArgumentException("Cannot msgpack object of type " + item.getClass().getCanonicalName())
      }
    }

    val unpackFuncMapBase = FastByteMap[UnpackFunc](
      MP_NULL   -> { in: DIS => null },
      MP_UINT64 -> { in: DIS =>   // Return a bigInt, it could be > Long.MaxValue
          val v = in.readLong()
          if (v >= 0) v
          else
            //this is a little bit more tricky, since we don't have unsigned longs
            math.BigInt(1, Array[Byte](((v >> 24) & 0xff).toByte,
                                       ((v >> 16) & 0xff).toByte,
                                       ((v >> 8) & 0xff).toByte, (v & 0xff).toByte))
                   },
      MP_INT64  -> { in: DIS => in.readLong() }
    ) ++ IntCodec.unpackFuncMap.mapAnyFunc ++
         BooleanCodec.unpackFuncMap.mapAnyFunc ++
         FloatCodec.unpackFuncMap.mapAnyFunc ++
         DoubleCodec.unpackFuncMap.mapAnyFunc ++
         seqCodec.unpackFuncMap.mapAnyFunc ++
         mapCodec.unpackFuncMap.mapAnyFunc

    val unpackFuncMap = unpackFuncMapBase ++ {
                        if (compatibilityMode) CompatibilityCodecs.ByteArrayCodec.unpackFuncMap.mapAnyFunc
                        else                   RawStringCodecs.allFuncMaps }
  }

  /**
   * Use this class when the K and V are not Any, however it cannot be used to initialize itself
   */
  class AnyCodec[K, V](val compatibilityMode: Boolean)
                      (implicit val kCodec: Codec[K],
                       implicit val vCodec: Codec[V]) extends AnyCodecBase[K, V] {
    implicit def keyCodec = kCodec
    implicit def valCodec = vCodec
  }


  /**
   * MUST USE def's in trait initialization otherwise you end up with nasty NPEs
   */
  object DefaultAnyCodec extends AnyCodecBase[Any, Any] {
    def compatibilityMode = false
    implicit def keyCodec = this
    implicit def valCodec = this
  }

  object DefaultAnyCodecCompat extends AnyCodecBase[Any, Any] {
    def compatibilityMode = true
    implicit def keyCodec = this
    implicit def valCodec = this
  }
}