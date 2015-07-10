package org.velvia.msgpack

import java.io.{DataInputStream => DIS, DataOutputStream}

object CollectionCodecs {
  import Format._

  // NOTE: Arrays automatically get converted to Seqs via WrappedArray
  class SeqCodec[T: Codec] extends Codec[Seq[T]] {
    def pack(out: DataOutputStream, s: Seq[T]) { packSeq(s, out) }

    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_ARRAY16 -> { in: DIS => unpackSeq(in.readShort() & MAX_16BIT, in) },
      MP_ARRAY32 -> { in: DIS => unpackSeq(in.readInt(), in) }
    ) ++ (0 to MAX_4BIT).map { len =>
      (MP_FIXARRAY | len).toByte -> { in: DIS => unpackSeq(len, in) }
    }
  }

  class MapCodec[K: Codec, V: Codec] extends Codec[Map[K, V]] {
    private val keyCodec = implicitly[Codec[K]]
    private val valCodec = implicitly[Codec[V]]

    def pack(out: DataOutputStream, m: Map[K, V]) { packMap(m, out) }

    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_MAP16 -> { in: DIS => unpackMap(in.readShort() & MAX_16BIT, in)(keyCodec, valCodec) },
      MP_MAP32 -> { in: DIS => unpackMap(in.readInt(), in)(keyCodec, valCodec) }
    ) ++ (0 to MAX_4BIT).map { len =>
      (MP_FIXMAP | len).toByte -> { in: DIS => unpackMap(len, in)(keyCodec, valCodec) }
    }
  }
}