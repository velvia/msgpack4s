package org.velvia.msgpack

import java.io.{DataInputStream => DIS, DataOutputStream}
import java.math.BigDecimal

object ExtraCodecs {
  import Format._
  import SimpleCodecs._
  import RawStringCodecs.ByteArrayCodec

  // BigDecimals are packed as an array of 2: [scale: Int, unscaledValue: ByteArray]
  implicit object BigDecimalCodec extends Codec[BigDecimal] {
    def pack(out: DataOutputStream, bd: BigDecimal) {
      out.write(0x2 | MP_FIXARRAY)
      packLong(bd.scale, out)
      packRawBytes(bd.unscaledValue.toByteArray, out)
    }
    val unpackFuncMap = Map[Byte, UnpackFunc](
      (MP_FIXARRAY | 0x2).toByte -> { in: DIS =>
        val scale = IntCodec.unpack(in)
        new BigDecimal(new java.math.BigInteger(ByteArrayCodec.unpack(in)), scale)
      }
    )
  }
}