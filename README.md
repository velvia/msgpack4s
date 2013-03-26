msgpack4s
=========

A super-simple MessagePack serialization library for Scala, based on msgpack-java-lite

* Two basic APIs: `MsgPack.pack(object)` and `MsgPack.unpack(byteArray)`.
* Directly unpacks maps, sequences, and any non-cyclic nested sequences/maps to Scala immutable collections
* Can unpack Map[Any, Any] or Map[String, Any], unlike msgpack-scala

Building, testing, packaging
============================

    sbt test
    sbt package