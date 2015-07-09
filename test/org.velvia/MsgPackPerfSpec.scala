package org.velvia

import util.Random
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class MsgPackPerfSpec extends FunSpec with ShouldMatchers {
  def genMap() = {
    Map("eventType" -> Random.nextInt(25),
        "user" -> "abcDEFghiDEF",
        "downloads" -> Map("bytes" -> 123456, "millis" -> Random.nextInt(50000)),
        "someList" -> List.fill(5)(Random.nextInt(16)))
  }

  import org.velvia.msgpack.CollectionCodecs._
  import org.velvia.msgpack.SimpleCodecs._
  import org.velvia.msgpack.RawStringCodecs._
  import org.velvia.msgpack.AnyCodecs._

  implicit val anyCodec = new AnyCodec[String, Any](false)(StringCodec, DefaultAnyCodec)
  implicit val mapCodec = new MapCodec[String, Any]

  def time(func: => Unit) {
    val startTime = System.nanoTime()
    var i = 0
    while (i < 10000) { func; i += 1 }
    val elapsed = System.nanoTime() - startTime
    println("Elapsed: %d ns, per-iteration: %f ns".format(elapsed, elapsed / 10000.0))
  }

  it("should pack pretty fast") {
    val map = genMap()
    println("Packing time:")
    time(msgpack.pack(map))
  }

  it("should unpack pretty fast") {
    val bytes = MsgPack.pack(genMap())
    println("Unpacking time:")
    time(msgpack.unpack(bytes)(mapCodec))
  }
}