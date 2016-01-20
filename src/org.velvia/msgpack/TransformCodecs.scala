package org.velvia.msgpack

import java.io.DataOutputStream


object TransformCodecs {

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