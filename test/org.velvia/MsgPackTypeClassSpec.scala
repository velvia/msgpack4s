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
}