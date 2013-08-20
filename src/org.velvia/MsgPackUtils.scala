package org.velvia

import java.io.DataInputStream

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

  def unpackInt(dis: DataInputStream): Int = getInt(unpack(dis))

  def unpackLong(dis: DataInputStream): Long = getLong(unpack(dis))

  def unpackSeq(dis: DataInputStream): Seq[Any] = unpack(dis).asInstanceOf[Seq[Any]]
}
