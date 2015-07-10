package org.velvia.msgpack

import com.rojoma.json.v3.ast._
import java.io.{DataInputStream => DIS, DataOutputStream}

/**
 * Codecs for serializing between MessagePack and rojoma-json AST
 */
object RojomaJsonCodecs {
  import Format._
  import SimpleCodecs._
  import RawStringCodecs.StringCodec
  import ExtraCodecs.BigDecimalCodec

  implicit object JNullCodec extends Codec[JNull] {
    def pack(out: DataOutputStream, item: JNull) { out.write(MP_NULL) }
    val unpackFuncMap = FastByteMap[UnpackFunc](
      MP_NULL -> { in: DIS => JNull }
    )
  }

  implicit object JBooleanCodec extends Codec[JBoolean] {
    def pack(out: DataOutputStream, item: JBoolean) { BooleanCodec.pack(out, item.boolean) }
    val unpackFuncMap = BooleanCodec.unpackFuncMap.mapValues(_.andThen(JBoolean(_)))
  }

  implicit object JStringCodec extends Codec[JString] {
    def pack(out: DataOutputStream, item: JString) { StringCodec.pack(out, item.string) }
    val unpackFuncMap = StringCodec.unpackFuncMap.mapValues(_.andThen(JString(_)))
  }

  implicit object JNumberCodec extends Codec[JNumber] {
    def pack(out: DataOutputStream, item: JNumber) { BigDecimalCodec.pack(out, item.toJBigDecimal) }
    val unpackFuncMap = BigDecimalCodec.unpackFuncMap.mapValues(_.andThen(JNumber(_)))
  }

  implicit object JArrayCodec extends Codec[JArray] {
    lazy val seqCodec = new CollectionCodecs.SeqCodec()(JValueCodec)
    def pack(out: DataOutputStream, a: JArray) { seqCodec.pack(out, a.toSeq) }
    lazy val unpackFuncMap = seqCodec.unpackFuncMap.mapValues(_.andThen(JArray(_)))
  }

  implicit object JObjectCodec extends Codec[JObject] {
    lazy val mapCodec = new CollectionCodecs.CMapCodec()(StringCodec, JValueCodec)
    def pack(out: DataOutputStream, m: JObject) { mapCodec.pack(out, m.fields) }
    lazy val unpackFuncMap = mapCodec.unpackFuncMap.mapValues(_.andThen(JObject(_)))
  }

  implicit object JValueCodec extends Codec[JValue] {
    def pack(out: DataOutputStream, item: JValue) {
      item match {
        case j: JString  => JStringCodec.pack(out, j)
        case j: JObject  => JObjectCodec.pack(out, j)
        case j: JNumber  => JNumberCodec.pack(out, j)
        case j: JArray   => JArrayCodec.pack(out, j)
        case j: JBoolean => JBooleanCodec.pack(out, j)
        case j: JNull    => JNullCodec.pack(out, j)
      }
    }
    val unpackFuncMap = JNullCodec.unpackFuncMap.mapAs[JValue] ++
                        JBooleanCodec.unpackFuncMap.mapAs[JValue] ++
                        JStringCodec.unpackFuncMap.mapAs[JValue] ++
                        JNumberCodec.unpackFuncMap.mapAs[JValue] ++
                        JArrayCodec.unpackFuncMap.mapAs[JValue] ++
                        JObjectCodec.unpackFuncMap.mapAs[JValue]
  }
}