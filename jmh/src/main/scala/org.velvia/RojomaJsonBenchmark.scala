package org.velvia

import com.rojoma.json.v3.io.{JsonReader, CompactJsonWriter}
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.{Mode, State, Scope}
import org.openjdk.jmh.annotations.OutputTimeUnit
import util.Random

import java.util.concurrent.TimeUnit

/**
 * Measures serialization speeds over an OutputStream API for a prototypical, string-heavy
 * 13KB JSON document.  Rojoma-JSON AST, Rojoma-JSON CompactWriter vs msgpack4s.
 *
 * For a description of the JMH measurement modes, see
 * https://github.com/ktoso/sbt-jmh/blob/master/src/sbt-test/sbt-jmh/jmh-run/src/main/scala/org/openjdk/jmh/samples/JMHSample_02_BenchmarkModes.scala
 */
@State(Scope.Thread)
class RojomaJsonBenchmark {
  // From checked in metadata JSON file, more typically JSON data
  val rawJsonStr = io.Source.fromURL(getClass.getResource("/metadata.json")).getLines.mkString("")

  val ast = JsonReader.fromString(rawJsonStr)
  val os = new org.apache.commons.io.output.NullOutputStream
  val dos = new java.io.DataOutputStream(os)
  val osw = new java.io.OutputStreamWriter(os)

  import org.velvia.msgpack.RojomaJsonCodecs._

  // According to @ktosopl, be sure to return some value if possible so that JVM won't
  // optimize out the method body.  However JMH is apparently very good at avoiding this.
  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def rojomaAstMsgpack(): Int = {
    var total = 0
    while (total < 100) {
      msgpack.pack(ast, dos)
      dos.flush()
      total += 1
    }
    total
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  def rojomaAstJson(): Int = {
    var total = 0
    while (total < 100) {
      CompactJsonWriter.toWriter(osw, ast)
      total += 1
    }
    total
  }
}
