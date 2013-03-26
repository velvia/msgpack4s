package com.github.velvia

import util.Random
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class MsgPackPerfSpec extends FunSpec with ShouldMatchers {
  def genMap() = {
    Map("eventType" -> Random.nextInt(25),
        "user" -> "abcDEFghiDEF",
        "downloads" -> Map("bytes" -> Random.nextInt(1000000), "millis" -> Random.nextInt(50000)),
        "someList" -> List.fill(5)(Random.nextInt(16)))
  }

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
    time(MsgPack.pack(map))
  }

  it("should unpack pretty fast") {
    val bytes = MsgPack.pack(genMap())
    println("Unpacking time:")
    time(MsgPack.unpack(bytes))
  }
}