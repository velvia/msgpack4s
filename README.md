msgpack4s
=========

A super-simple MessagePack serialization library for Scala, based on [msgpack-java-lite](https://bitbucket.org/sirbrialliance/msgpack-java-lite/overview)

* Simple API: `MsgPack.pack(object)` and `MsgPack.unpack(byteArray)`.
* Directly unpacks maps, sequences, and any non-cyclic nested sequences/maps to Scala immutable collections
* Can unpack `Map[Any, Any]` or `Map[String, Any]` without extra code, unlike msgpack-scala
* No extra dependencies.  No need to build a separate Java library.
* significantly (3x as of 3/29/13) faster than msgpack-scala for unpacking Maps

For the exact semantics of packing and unpacking, see the ScalaDoc.

Using this library
==================

Include this in `build.sbt`:

```scala
repositories += "velvia maven" at "http://dl.bintray.com/velvia/maven"

libraryDependencies += "org.velvia" %% "msgpack4s" % "0.4.2"
```

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

val item1: String = MsgPack.unpack(inStream, UNPACK_RAW_AS_STRING)
val item2: Int = MsgPack.unpack(inStream, 0)
val item3: Long = MsgPack.unpack(inStream, 0)
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

Building, testing, packaging
============================

    sbt test
    sbt "+ package"
    sbt "+ make-pom"
