msgpack4s
=========

A super-simple MessagePack serialization library for Scala.

* Simple, type-safe API
* Extensible via `Codec` type classes
* Designed and tested for long binary streaming applications
* Built-in support for [Json4s](http://github.com/json4s/json4s) and [rojoma-json](http://github.com/rjmac/rojoma-json) ASTs... easily supercharge your JSON Scala apps!
    - 10x speedup over Json4s for double-heavy applications such as GeoJSON
    - Over 2x speedup for regular string-heavy JSON documents
* Directly unpacks maps, sequences, and any non-cyclic nested sequences/maps to Scala immutable collections
* Can unpack `Map[Any, Any]` or `Map[String, Any]` without extra code, unlike msgpack-scala
* No extra dependencies.  No need to build a separate Java library.
* significantly (3x as of 3/29/13) faster than msgpack-scala for unpacking Maps

For the exact semantics of packing and unpacking, see the ScalaDoc.

Using this library
==================

Include this in `build.sbt`:

```scala
resolvers += "velvia maven" at "http://dl.bintray.com/velvia/maven"

libraryDependencies += "org.velvia" %% "msgpack4s" % "0.4.3"
```

Currently cross-compiled for Scala 2.10 and 2.11.

You will probably want to use the type-safe API that uses TypeClasses:

```scala
import org.velvia.msgpack._
import org.velvia.msgpack.SimpleCodecs._

val byteArray = pack(123)
val num = unpack[Int](byteArray)
```

Serializing maps and sequences takes a bit more work, to instantiate a codec that has the specific types.  The reward is speed when your collections have specific well-known types -- if you reuse the codecs.  You can also use the older non-type-safe Any APIs (ex. `MsgPack.pack(...)`), which use `AnyCodecs.DefaultAnyCodec`, but these are slower because they assume both Maps and Seqs have Anys.

```scala
import org.velvia.msgpack.CollectionCodecs._

val intSeqCodec = new SeqCodec[Int]

val seq1 = Seq(1, 2, 3, 4, 5)
unpack(pack(seq1)(intSeqCodec))(intSeqCodec) should equal (seq1)
```

Serializing Json4s and rojoma-json ASTs is easy.  See the example in `MsgPackTypeClassSpec` -- all you need to do is import from the right codecs.

There are also the older APIs that work with Any, but are not type safe.  They also are not extensible to custom objects the way the type-safe APIs are.

```scala
import org.velvia.MsgPack
MsgPack.unpack(MsgPack.pack(Map("key" -> 3)))
```

Streaming mode
==============

msgpack4s is under the covers designed to work with streams - in fact it works great for very long binary streams and has been tested with that in mind.  Even the byte array APIs just wrap the streaming APIs with a `ByteArrayOutputStream`.  Here is how to use it in streaming mode, including ensuring that the streams get closed properly at the end or in case of failure:

```scala
import com.rojoma.simplearm.util._
import java.io.DataOutputStream

    for {
      os <- managed(resp.getOutputStream)
      dos <- managed(new DataOutputStream(os))
      data <- listOfObjects
    } {
      msgpack.pack(data, dos)
    }
```

Convenience Functions
=====================

For the older Any-based API, `MsgPackUtils` has convenience functions so you can pull out the right types from `unpack` without needing
verbose `isInstanceOf[..]`.  They are especially useful when working with numbers.  For example:

```scala
import org.velvia.MsgPackUtils._
val map = unpackMap(bytes)
println("My number = " + map.asInt("number") + 99)
```

There are also functions `getInt` and `getLong` to conveniently get an Int or Long out, because MessagePack will pack them as [U]INT8/16/32/64's.

Compatibility Mode
==================
The MessagePack format was upgraded to differentiate STRings vs RAWs.  So now 
one no longer has to pass in an option to decide how to unpack strings vs raw bytes.
OTOH the unpack interface has been changed to provide a compatibility mode:
setting to true allows to parse STR formats as raw bytes, so that MessagePack messages
sent using older encoders can be parsed as raw bytes if needed.

To unpack older MessagePack messages as raw bytes instead of strings:

   MsgPack.unpack(inStream, true)

Running Perf Tests
==================
msgpack4s comes with several benchmarks in the jmh project.  They are used to compare Json4s, rojoma-json, to msgpack4s.  To run them with profiling:

    jmh/run -wi 5 -i 5 -prof stack -jvmArgsAppend -Djmh.stack.lines=7

You can also pass a regex at the end to limit which benchmarks to run.

Building, testing, packaging
============================

    sbt test
    sbt "+ package"
    sbt "+ make-pom"
