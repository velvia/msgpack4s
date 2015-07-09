package org.velvia

import org.scalatest.FunSpec
import org.scalatest.Matchers

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
  }

  describe("collections packing and unpacking") {
    import org.velvia.msgpack.CollectionCodecs._

    val intSeqCodec = new SeqCodec[Int]
    val strIntMapCodec = new MapCodec[String, Int]

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
  }
}