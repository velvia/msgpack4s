package org.velvia

import java.io.{ByteArrayInputStream, DataInputStream}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSpec

class MsgPackUtilsSpec extends FunSpec with ShouldMatchers {

  val seqIntRow = (Seq(10, "AZ", "mobile"), 8)

  describe("Test MsgPackUtils Array[Byte] methods") {
    it("serialize a Int and de-serialize it back") {
      val number = 16
      MsgPackUtils.unpackInt(MsgPack.pack(number)) should equal (number)
    }

    it("serialize a Long and de-serialize it back") {
      val number:Long = 100000000000L
      MsgPackUtils.unpackLong(MsgPack.pack(number)) should equal (number)
    }

    it("serialize a Seq[Any] and de-serialize it back") {
      val seq = Seq(10, "AZ", "mobile")
      val serializedBytes = MsgPack.pack(seq)

      // unpack
      MsgPack.unpack(serializedBytes) should equal (seq)

      // unpackSeq
      val deserialzied = MsgPackUtils.unpackSeq(serializedBytes)
      deserialzied(0) should equal (10)
      deserialzied(1) should equal ("AZ")
      deserialzied(2) should equal ("mobile")
    }

    // Don't need test multiple types as Array[Byte] can't support.
  }

  describe("Test MsgPackUtils DataInputStream methods") {
    it("serialize a Int and de-serialize it back") {
      val number = 16
      val dis = new DataInputStream(new ByteArrayInputStream(MsgPack.pack(number)))

      MsgPackUtils.unpackInt(dis) should equal (number)
    }

    it("serialize a Long and de-serialize it back") {
      val number:Long = 100000000000L
      val dis = new DataInputStream(new ByteArrayInputStream(MsgPack.pack(number)))

      MsgPackUtils.unpackLong(dis) should equal (number)
    }

    it("serialize a Seq[Any] and de-serialize it back") {
      val seq = Seq(10, "AZ", "mobile")
      val dis = new DataInputStream(new ByteArrayInputStream(MsgPack.pack(seq)))

      val deserialized = MsgPackUtils.unpackSeq(dis)

      deserialized should equal (seq)
      deserialized(0) should equal (10)
      deserialized(1) should equal ("AZ")
      deserialized(2) should equal ("mobile")
    }

    it("serialize multiple types and de-serialize them back") {
      val intNumber = 16
      val seq = Seq(10, "AZ", "mobile")
      val longNumber = 100000000000L

      val rawBytes = MsgPack.pack(intNumber) ++
        MsgPack.pack(seq) ++
        MsgPack.pack(longNumber)
      val dis = new DataInputStream(new ByteArrayInputStream(rawBytes))

      MsgPackUtils.unpackInt(dis) should equal (16)
      MsgPackUtils.unpackSeq(dis) should equal (seq)
      MsgPackUtils.unpackLong(dis) should equal (100000000000L)
    }
  }

}