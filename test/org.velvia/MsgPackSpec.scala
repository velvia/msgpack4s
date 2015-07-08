package org.velvia

import util.Random
import org.scalatest.FunSpec
import org.scalatest.Matchers

class MsgPackSpec extends FunSpec with Matchers {
  describe("MsgPack pack and unpack") {
    it("should serialize and deserialize integers") {
      val numbers = Seq(0, 2, -3, 31, 32, 33, -32, -33, 127, 128, 129, -127, -128, -129,
                        255, 256, 257, -255, -256, -257,
                        0xfffe, 0xffff, 0x10000, -0xffff, -0x10000, 0xffffffff, 0x100000000L)
      numbers foreach { n =>
        val packed = MsgPack.pack(n)
        MsgPack.unpack(packed) should equal (n)
      }
    }

    it("should serialize and deserialize floats and doubles") {
      val numbers = Seq[Number](
        0.0, 1.0, -1.0,
        0.0f, 1.0f, -1.0f,
        Double.NaN, Double.NegativeInfinity, Double.PositiveInfinity,
        Double.MinValue, Double.MaxValue, Double.MinPositiveValue,
        Float.NaN, Float.NegativeInfinity, Float.PositiveInfinity,
        Float.MinValue, Float.MaxValue, Float.MinPositiveValue)
      numbers foreach { n =>
        val unpacked = MsgPack.unpack(MsgPack.pack(n))
        assert(unpacked.equals(n))    /// For some reason, should equal or === does not work
      }
    }

    it("should serialize and deserialize strings") {
      MsgPack.unpack(MsgPack.pack("abcDEF")) should equal ("abcDEF")
    }

    it("should deserialize older MsgPack messages as raw bytes in compatibility mode") {
      val bais = new java.io.ByteArrayInputStream(MsgPack.pack("abcDEF"))
      MsgPack.unpack(new java.io.DataInputStream(bais), true) should equal ("abcDEF".getBytes("UTF-8"))
    }

    it("should serialize and deserialize Bools, null") {
      MsgPack.unpack(MsgPack.pack(true)) should equal (true)
      MsgPack.unpack(MsgPack.pack(false)) should equal (false)
      MsgPack.unpack(MsgPack.pack(null)) should equal (null.asInstanceOf[AnyRef])
    }

    it("should serialize and deserialize sequences") {
      val lengths = Seq(1, 2, 15, 16, 31, 32, 0xffff, 0x10000)
      lengths foreach { len =>
        val list = Seq.fill(len)(Random.nextInt(100))
        val unpacked = MsgPack.unpack(MsgPack.pack(list)).asInstanceOf[Seq[Int]]
        unpacked.getClass should equal (classOf[Vector[Int]])
        unpacked should have length (len)
        unpacked should equal (list)
      }
    }

    it("should serialize and deserialize arrays, Nil, mixed sequences") {
      val array = Array(1, 2, -3)
      val unpacked = MsgPack.unpack(MsgPack.pack(array)).asInstanceOf[Seq[Any]]
      unpacked.getClass should equal (classOf[Vector[Int]])
      unpacked should have length (array.size)
      assert(unpacked.sameElements(array))

      val nil = Nil
      val unpacked1 = MsgPack.unpack(MsgPack.pack(nil)).asInstanceOf[Seq[Any]]
      unpacked1 should have length (0)

      val vec = Vector("a", 4, true, null)
      val unpacked2 = MsgPack.unpack(MsgPack.pack(vec)).asInstanceOf[Seq[Any]]
      unpacked2 should have length (vec.length)
      unpacked2 should equal (vec)
    }

    it("should serialize and deserialize Maps") {
      val map = Map("type" -> 9, "owner" -> "ev", "stats" -> Map(29 -> 1, "those" -> Seq(1, 2)))
      val unpacked = MsgPack.unpack(MsgPack.pack(map)).asInstanceOf[Map[_, _]]
      unpacked should equal (map)
    }
  }
}