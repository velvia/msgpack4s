package org.velvia

import org.scalatest.FunSpec
import org.scalatest.Matchers

class MsgPackTypeClassSpec extends FunSpec with Matchers {
  import org.velvia.msgpack._
  import org.velvia.msgpack.SimpleCodecs._

  describe("primitive types unpacking and packing") {
    it("should unpack and pack bool values") {
      unpack[Boolean](pack(true)) should equal (true)
      unpack[Boolean](pack(false)) should equal (false)
    }

    it("should unpack and pack primitive numbers") {
      unpack[Int](pack(Int.MaxValue)) should equal (Int.MaxValue)
      unpack[Int](pack(-1)) should equal (-1)
    }
  }
}