package org.velvia

import java.io.DataInputStream

/**
 * Some convenient methods for unpacking things
 */
object MsgPackUtils {
  def unpack(dis: DataInputStream): Any = MsgPack.unpack(dis, MsgPack.DEFAULT_OPTIONS)

  def getInt(item: Any): Int = item match {
    case i: Int   => i
    case b: Byte  => b.toInt
    case s: Short => s.toInt
    case x: Any   => throw new ClassCastException("Can't convert " + x.getClass.getName
      + " to an Int")
  }

  def getLong(item: Any): Long = item match {
    case i: Int   => i.toLong
    case l: Long  => l
    case b: Byte  => b.toLong
    case s: Short => s.toLong
    case x: Any   => throw new ClassCastException("Can't convert " + x.getClass.getName
      + " to a Long")
  }

  def unpackInt(rawData: Array[Byte]): Int = getInt(MsgPack.unpack(rawData))

  def unpackLong(rawData: Array[Byte]): Long = getLong(MsgPack.unpack(rawData))

  def unpackSeq(rawData: Array[Byte]): Seq[Any] = MsgPack.unpack(rawData).asInstanceOf[Seq[Any]]

  def unpackMap(rawData: Array[Byte]): Map[Any, Any] = MsgPack.unpack(rawData).asInstanceOf[Map[Any, Any]]

  def unpackInt(dis: DataInputStream): Int = getInt(unpack(dis))

  def unpackLong(dis: DataInputStream): Long = getLong(unpack(dis))

  def unpackSeq(dis: DataInputStream): Seq[Any] = unpack(dis).asInstanceOf[Seq[Any]]

  def unpackMap(dis: DataInputStream): Map[Any, Any] = unpack(dis).asInstanceOf[Map[Any, Any]]

  /**
   * Implicit conversions so we can access elements of the map through below extension methods
   */
  implicit def toMapWrapper[K](map: collection.Map[K, Any]) = new MsgPackMapWrapper(map)
}

/**
 * A wrapper around maps to provide convenient extension methods
 */
class MsgPackMapWrapper[K](map: collection.Map[K, Any]) {
  def as[T](key: K): T = map(key).asInstanceOf[T]

  def asOpt[T](key: K): Option[T] = map.get(key).asInstanceOf[Option[T]]

  def asMap(key: K): Map[K, Any] = map(key).asInstanceOf[Map[K, Any]]

  def asInt(key: K): Int = MsgPackUtils.getInt(map(key))

  def asLong(key: K): Long = MsgPackUtils.getLong(map(key))
}
