package org.velvia.msgpack

import java.io.{DataInputStream => DIS, DataOutputStream}

object TupleCodecs {
  class TupleCodec2[A1: Codec, A2: Codec] extends Codec[(A1, A2)] {

    private val codec1 = implicitly[Codec[A1]]
    private val codec2 = implicitly[Codec[A2]]

    def pack(out: DataOutputStream, item: (A1, A2)): Unit = {
      out.write(0x02 | Format.MP_FIXARRAY)
      codec1.pack(out, item._1)
      codec2.pack(out, item._2)
    }

    val unpackFuncMap = FastByteMap[UnpackFunc](
      (0x02 | Format.MP_FIXARRAY).toByte -> { in: DIS =>
        val r1 = codec1.unpack(in)
        val r2 = codec2.unpack(in)
        (r1, r2)
      }
    )
  }

  class TupleCodec3[A1: Codec, A2: Codec, A3: Codec]
    extends Codec[(A1, A2, A3)] {

    private val codec1 = implicitly[Codec[A1]]
    private val codec2 = implicitly[Codec[A2]]
    private val codec3 = implicitly[Codec[A3]]

    def pack(out: DataOutputStream, item: (A1, A2, A3)): Unit = {
      out.write(0x03 | Format.MP_FIXARRAY)
      codec1.pack(out, item._1)
      codec2.pack(out, item._2)
      codec3.pack(out, item._3)
    }

    val unpackFuncMap = FastByteMap[UnpackFunc](
      (0x03 | Format.MP_FIXARRAY).toByte -> { in: DIS =>
        val r1 = codec1.unpack(in)
        val r2 = codec2.unpack(in)
        val r3 = codec3.unpack(in)
        (r1, r2, r3)
      }
    )
  }

  class TupleCodec4[A1: Codec, A2: Codec, A3: Codec, A4: Codec]
    extends Codec[(A1, A2, A3, A4)] {

    private val codec1 = implicitly[Codec[A1]]
    private val codec2 = implicitly[Codec[A2]]
    private val codec3 = implicitly[Codec[A3]]
    private val codec4 = implicitly[Codec[A4]]

    def pack(out: DataOutputStream, item: (A1, A2, A3, A4)): Unit = {
      out.write(0x04 | Format.MP_FIXARRAY)
      codec1.pack(out, item._1)
      codec2.pack(out, item._2)
      codec3.pack(out, item._3)
      codec4.pack(out, item._4)
    }

    val unpackFuncMap = FastByteMap[UnpackFunc](
      (0x04 | Format.MP_FIXARRAY).toByte -> { in: DIS =>
        val r1 = codec1.unpack(in)
        val r2 = codec2.unpack(in)
        val r3 = codec3.unpack(in)
        val r4 = codec4.unpack(in)
        (r1, r2, r3, r4)
      }
    )
  }

  // TODO: add TupleCodec5, TupleCodec6 ... and so on
}
