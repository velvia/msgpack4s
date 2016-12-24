package org.velvia.msgpack

import java.io.{DataInputStream => DIS, DataOutputStream}
import org.json4s.JsonAST._

/**
 * Codecs for serializing between MessagePack and Json4s AST
 */
object Json4sCodecs {
  import Format._
  import SimpleCodecs._
  import RawStringCodecs.StringCodec
  import ExtraCodecs._

  implicit object JBoolCodec extends Codec[JBool] {
    def pack(out: DataOutputStream, item: JBool) { BooleanCodec.pack(out, item.value) }
    val unpackFuncMap = BooleanCodec.unpackFuncMap.mapValues(_.andThen(JBool(_)))
  }

  implicit object JStringCodec extends Codec[JString] {
    def pack(out: DataOutputStream, item: JString) { StringCodec.pack(out, item.s) }
    val unpackFuncMap = StringCodec.unpackFuncMap.mapValues(_.andThen(JString(_)))
  }

  implicit object JDecimalCodec extends Codec[JDecimal] {
    def pack(out: DataOutputStream, item: JDecimal) { BigDecimalCodec.pack(out, item.num.underlying) }
    val unpackFuncMap = BigDecimalCodec.unpackFuncMap.mapValues(_.andThen(b => JDecimal(BigDecimal(b))))
  }

  implicit object JDoubleCodec extends Codec[JDouble] {
    def pack(out: DataOutputStream, item: JDouble) { DoubleCodec.pack(out, item.num) }
    val unpackFuncMap = DoubleCodec.unpackFuncMap.mapValues(_.andThen(JDouble(_)))
  }

  implicit object JIntCodec extends Codec[JInt] {
    def pack(out: DataOutputStream, item: JInt) { BigIntegerCodec.pack(out, item.num.underlying) }
    val unpackFuncMap = BigIntegerCodec.unpackFuncMap.mapValues(_.andThen(JInt(_)))
  }

  implicit object JLongCodec extends Codec[JLong] {
    def pack(out: DataOutputStream, item: JLong) { LongCodec.pack(out, item.num) }
    val unpackFuncMap = LongCodec.unpackFuncMap.mapValues(_.andThen(JLong(_)))
  }

  implicit object JArrayCodec extends Codec[JArray] {
    lazy val seqCodec = new CollectionCodecs.SeqCodec()(JValueCodec)
    def pack(out: DataOutputStream, a: JArray) { seqCodec.pack(out, a.arr) }
    lazy val unpackFuncMap = seqCodec.unpackFuncMap.mapValues(_.andThen(s => JArray(s.toList)))
  }

  implicit object JSetCodec extends Codec[JSet] {
    lazy val seqCodec = new CollectionCodecs.SeqCodec()(JValueCodec)
    def pack(out: DataOutputStream, a: JSet) { seqCodec.pack(out, a.set.toSeq) }
    lazy val unpackFuncMap = seqCodec.unpackFuncMap.mapValues(_.andThen(s => JSet(s.toSet)))
  }

  implicit object JObjectCodec extends Codec[JObject] {
    lazy val mapCodec = new CollectionCodecs.MapCodec()(StringCodec, JValueCodec)
    def pack(out: DataOutputStream, m: JObject) { packMapSeq[String, JValue](m.obj, out) }
    lazy val unpackFuncMap = mapCodec.unpackFuncMap.mapValues(_.andThen(m => JObject(m.toList)))
  }

  implicit object JValueCodec extends Codec[JValue] {
    def pack(out: DataOutputStream, item: JValue) {
      item match {
        case j: JString  => JStringCodec.pack(out, j)
        case j: JObject  => JObjectCodec.pack(out, j)
        case j: JDouble  => JDoubleCodec.pack(out, j)
        case j: JInt     => JIntCodec.pack(out, j)
        case j: JLong    => JLongCodec.pack(out, j)
        case j: JSet     => JSetCodec.pack(out, j)
        case j: JArray   => JArrayCodec.pack(out, j)
        case j: JDecimal => JDecimalCodec.pack(out, j)
        case j: JBool    => JBoolCodec.pack(out, j)
        case JNull | JNothing => out.write(MP_NULL)
      }
    }
    val unpackFuncMap = FastByteMap[UnpackFunc](
                          MP_NULL -> { in: DIS => JNull }
                        ) ++
                        JBoolCodec.unpackFuncMap.mapAs[JValue] ++
                        JStringCodec.unpackFuncMap.mapAs[JValue] ++
                        JDecimalCodec.unpackFuncMap.mapAs[JValue] ++
                        JDoubleCodec.unpackFuncMap.mapAs[JValue] ++
                        JIntCodec.unpackFuncMap.mapAs[JValue] ++
                        JLongCodec.unpackFuncMap.mapAs[JValue] ++
                        JSetCodec.unpackFuncMap.mapAs[JValue] ++
                        JArrayCodec.unpackFuncMap.mapAs[JValue] ++
                        JObjectCodec.unpackFuncMap.mapAs[JValue]
  }
}