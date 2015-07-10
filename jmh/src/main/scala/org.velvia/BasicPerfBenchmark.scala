package org.velvia

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.{Mode, State, Scope}
import org.openjdk.jmh.annotations.OutputTimeUnit
import util.Random

import java.util.concurrent.TimeUnit

/**
 * Measures basic read benchmark with no NAs for an IntColumn.
 * Just raw read speed basically.
 *
 * For a description of the JMH measurement modes, see
 * https://github.com/ktoso/sbt-jmh/blob/master/src/sbt-test/sbt-jmh/jmh-run/src/main/scala/org/openjdk/jmh/samples/JMHSample_02_BenchmarkModes.scala
 */
@State(Scope.Thread)
class BasicPerfBenchmark {
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

  val map = genMap()
  val bytes = MsgPack.pack(genMap())

  // According to @ktosopl, be sure to return some value if possible so that JVM won't
  // optimize out the method body.  However JMH is apparently very good at avoiding this.
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def packMap(): Int = {
    var total = 0
    while (total < 100) {
      msgpack.pack(map)
      total += 1
    }
    total
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def unpackMap(): Int = {
    var total = 0
    while (total < 100) {
      msgpack.unpack(bytes)(mapCodec)
      total += 1
    }
    total
  }
}
