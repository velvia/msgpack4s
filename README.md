msgpack4s
=========

A super-simple MessagePack serialization library for Scala, based on [msgpack-java-lite](https://bitbucket.org/sirbrialliance/msgpack-java-lite/overview)

* Two basic APIs: `MsgPack.pack(object)` and `MsgPack.unpack(byteArray)`.
* Directly unpacks maps, sequences, and any non-cyclic nested sequences/maps to Scala immutable collections
* Can unpack Map[Any, Any] or Map[String, Any] without extra code, unlike msgpack-scala
* No extra dependencies
* Performance competitive with msgpack-scala

For the exact semantics of packing and unpacking, see the ScalaDoc.

Building, testing, packaging
============================

    sbt test
    sbt "+ package"