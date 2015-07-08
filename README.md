msgpack4s
=========

A super-simple MessagePack serialization library for Scala, based on [msgpack-java-lite](https://bitbucket.org/sirbrialliance/msgpack-java-lite/overview)

* Simple API: `MsgPack.pack(object)` and `MsgPack.unpack(stream)`.
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

Streaming mode
==============

There is a streaming API available as well.  This may be useful for cases where you need to unpack bytes that
were written with multiple `pack()` calls.  For example:

```scala
val outStream = new DataOutputStream(...)
MsgPack.pack(item1, outStream)
MsgPack.pack(item2, outStream)
MsgPack.pack(item3, outStream)

....

val item1: String = MsgPack.unpack(inStream)
val item2: Int = MsgPack.unpack(inStream)
val item3: Long = MsgPack.unpack(inStream)
```

Convenience Functions
=====================

MsgPackUtils has convenience functions so you can pull out the right types from `unpack` without needing
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

Building, testing, packaging
============================

    sbt test
    sbt "+ package"
    sbt "+ make-pom"
