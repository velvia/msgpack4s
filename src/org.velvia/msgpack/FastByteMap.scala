package org.velvia.msgpack

import java.io.{DataInputStream => DIS}

case class FastByteMap[V](things: (Byte, V)*) {
  val aray = new Array[Any](256)
  things.foreach { case (k, v) => aray(k & 0x00ff) = v }

  def ++(other: FastByteMap[V]): FastByteMap[V] = {
    val items = for { i <- 0 to 255 if other.aray(i) != null || aray(i) != null
                    } yield {
      i.toByte -> { if (other.aray(i) != null) other.aray(i) else aray(i) }
    }
    FastByteMap[Any](items:_*).asInstanceOf[FastByteMap[V]]
  }

  def ++(items: Seq[(Byte, V)]): FastByteMap[V] = ++(FastByteMap[V](items:_*))

  def mapValues[W](func: V => W): FastByteMap[W] = {
    val items = for { i <- 0 to 255 if aray(i) != null } yield {
      i.toByte -> func(aray(i).asInstanceOf[V])
    }
    FastByteMap[Any](items:_*).asInstanceOf[FastByteMap[W]]
  }

  def mapAnyFunc = mapValues(_.asInstanceOf[DIS => Any])
  def mapAs[V] = mapValues(_.asInstanceOf[DIS => V])

  def getOrElse(b: Byte, default: => V): V = {
    val v = aray(b & 0x00ff)
    if (v != null) v.asInstanceOf[V] else default
  }
}

