package org.velvia

import java.math.{BigDecimal, BigInteger}
import org.scalatest.FunSpec
import org.scalatest.Matchers

import scala.util.Random

class MsgPackTypeClassSpec extends FunSpec with Matchers {
  import org.velvia.msgpack._
  import org.velvia.msgpack.SimpleCodecs._
  import org.velvia.msgpack.RawStringCodecs._

  describe("basic types unpacking and packing") {
    it("should unpack and pack bool values") {
      unpack[Boolean](pack(true)) should equal (true)
      unpack[Boolean](pack(false)) should equal (false)
    }

    it("should unpack and pack primitive numbers") {
      unpack[Int](pack(Int.MaxValue)) should equal (Int.MaxValue)
      unpack[Int](pack(-1)) should equal (-1)
      unpack[Int](pack(-33)) should equal (-33)

      unpack[Byte](pack(-34.toByte)) should equal (-34.toByte)
      unpack[Short](pack(128.toShort)) should equal (128.toShort)

      unpack[Long](pack(-35L)) should equal (-35L)
      unpack[Long](pack(-1000L)) should equal (-1000L)
    }

    it("should unpack and pack floats and doubles") {
      unpack[Float](pack(3.141F)) should equal (3.141F)
      unpack[Double](pack(Math.E)) should equal (Math.E)
    }

    it("should unpack and pack strings") {
      unpack[String](pack("abcde")) should equal ("abcde")
      val longerStr = "The quick brown fox jumped over the lazy fence"
      unpack[String](pack(longerStr)) should equal (longerStr)
    }

    it("should unpack and pack Unicode strings") {
      val korean = "안녕"
      unpack[String](pack(korean)) should equal (korean)
    }
  }

  describe("collections packing and unpacking") {
    import org.velvia.msgpack.CollectionCodecs._

    val intSeqCodec = new SeqCodec[Int]
    val strIntMapCodec = new MapCodec[String, Int]
    val intSetCodec = new SetCodec[Int]

    it("should pack and unpack Seqs and Arrays") {
      val seq1 = Seq(1, 2, 3, 4, 5)
      unpack(pack(seq1)(intSeqCodec))(intSeqCodec) should equal (seq1)

      val array = (-3 to 24).toArray   // Force to be longer than 16 values
      unpack(pack(array.toSeq)(intSeqCodec))(intSeqCodec) should equal (array.toSeq)
    }

    it("should pack and unpack Maps") {
      val map = Map("apples" -> 1, "bears" -> -5, "oranges" -> 100)
      unpack(pack(map)(strIntMapCodec))(strIntMapCodec) should equal (map)
    }

    it("should pack and unpack Sets") {
      val set = Set(1, 2, 3, 4, 5)
      unpack(pack(set)(intSetCodec))(intSetCodec) should equal (set)
    }
  }

  describe("extras packing and unpacking") {
    import org.velvia.msgpack.ExtraCodecs._

    it("should pack and unpack BigDecimal") {
      unpack[BigDecimal](pack(new BigDecimal(1000))) should equal (new BigDecimal(1000))
      val bdPi = new BigDecimal(Math.PI)
      unpack[BigDecimal](pack(bdPi)) should equal (bdPi)
    }

    it("should pack and unpack BigInteger") {
      unpack[BigInteger](pack(new BigInteger("1000"))) should equal (new BigInteger("1000"))
    }
  }

  describe("Json4S AST packing and unpacking") {
    import org.json4s.native.JsonMethods._
    import org.json4s._
    import org.json4s.JsonAST._
    import msgpack.Json4sCodecs._

    val aray = parse("""[1, 2.5, null, "Streater"]""")
    val map = parse("""{"bool": true, "aray": [3, -4], "map": {"inner": "me"}}""")

    it("should pack and unpack Json4S AST") {
      unpack[JArray](pack(aray)) should equal (aray)
      unpack[JValue](pack(aray)) should equal (aray)
      unpack[JValue](pack(map)) should equal (map)
    }
  }

  describe("tuple packing and unpacking") {
    import org.velvia.msgpack.TupleCodecs._

    it("should pack and unpack Tuple2") {
      val codec2 = new TupleCodec2[Int, Int]
      val tuple2 = (Random.nextInt(), Random.nextInt())
      val unpacked2 = unpack(pack(tuple2)(codec2))(codec2)
      unpacked2.getClass should equal (classOf[(Int, Int)])
      unpacked2 should equal (tuple2)
    }

    it("should pack and unpack Tuple3") {
      val codec3 = new TupleCodec3[Int, Int, Int]
      val tuple3 = (Random.nextInt(), Random.nextInt(), Random.nextInt())
      val unpacked3 = unpack(pack(tuple3)(codec3))(codec3)
      unpacked3.getClass should equal (classOf[(Int, Int, Int)])
      unpacked3 should equal (tuple3)
    }

    it("should pack and unpack Tuple4") {
      val codec4 = new TupleCodec4[Int, Int, Int, Int]
      val tuple4 = (Random.nextInt(), Random.nextInt(), Random.nextInt(), Random.nextInt())
      val unpacked4 = unpack(pack(tuple4)(codec4))(codec4)
      unpacked4.getClass should equal (classOf[(Int, Int, Int, Int)])
      unpacked4 should equal (tuple4)
    }
  }

  describe("case class packing and unpacking") {
    import org.velvia.msgpack.CaseClassCodecs._

    it("should pack and unpack case class of 1 parameter") {
      case class C1(a: Int)
      val codec = new CaseClassCodec1[C1, Int](C1.apply, C1.unapply)
      val c = C1(Random.nextInt())
      val unpacked = unpack(pack(c)(codec))(codec)
      unpacked.getClass should equal (classOf[C1])
      unpacked should equal (c)
    }

    it("should pack and unpack case class of 2 parameters") {
      case class C2(a1: Int, a2: Int)
      val codec2 = new CaseClassCodec2[C2, Int, Int](C2.apply, C2.unapply)
      val c = C2(Random.nextInt(), Random.nextInt())
      val unpacked = unpack(pack(c)(codec2))(codec2)
      unpacked.getClass should equal (classOf[C2])
      unpacked should equal (c)
    }

    it("should pack and unpack case class of 3 parameters") {
      case class C3(a1: Int, a2: Int, a3: Int)
      val codec = new CaseClassCodec3[C3, Int, Int, Int](C3.apply, C3.unapply)
      val c = C3(Random.nextInt(), Random.nextInt(), Random.nextInt())
      val unpacked = unpack(pack(c)(codec))(codec)
      unpacked.getClass should equal (classOf[C3])
      unpacked should equal (c)
    }

    it("should pack and unpack case class of 4 parameters") {
      case class C4(a1: Int, a2: Int, a3: Int, a4: Int)
      val codec = new CaseClassCodec4[C4, Int, Int, Int, Int](C4.apply, C4.unapply)
      val c = C4(Random.nextInt(), Random.nextInt(), Random.nextInt(), Random.nextInt())
      val unpacked = unpack(pack(c)(codec))(codec)
      unpacked.getClass should equal (classOf[C4])
      unpacked should equal (c)
    }
  }
}