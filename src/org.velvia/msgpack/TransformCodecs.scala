package org.velvia.msgpack

import java.io.DataOutputStream


object TransformCodecs {

  /** Defines a codec for `A` given one for `B` and a bijection `A` <=> `B`.
    *
    * An example would be (de)serializing a `Date` by storing its timestamp,
    * which is just a `Long`. This would look like:
    *
    *     import java.util.Date
    *     val c: Codec[Date] = new TransformCodec[Date, Long](_.getTime, new Date(_))
    */
  class TransformCodec[A, B: Codec](
    forward: A => B,
    backward: B => A
  ) extends Codec[A] {

    private val codec1 = implicitly[Codec[B]]

    override def pack(out: DataOutputStream, item: A): Unit =
      codec1.pack(out, forward(item))

    val unpackFuncMap = {
      val things = codec1.unpackFuncMap.things map { case (byte, func) =>
        byte -> func.andThen(backward)
      }

      FastByteMap[UnpackFunc](things: _*)
    }
  }
}