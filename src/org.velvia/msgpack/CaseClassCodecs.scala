package org.velvia.msgpack

import java.io.{DataInputStream => DIS, DataOutputStream}

import org.velvia.msgpack.TupleCodecs.{TupleCodec4, TupleCodec3, TupleCodec2}

object CaseClassCodecs {

  class CaseClassCodec1[T, A](
    apply: A => T,
    unapply: T => Option[A]
  )(implicit K: Codec[A]) extends Codec[T] {

    private val codec1 = implicitly[Codec[A]]

    override def pack(out: DataOutputStream, item: T): Unit = {
      out.write(0x01 | Format.MP_FIXARRAY)
      codec1.pack(out, unapply(item).get)
    }
    val unpackFuncMap = FastByteMap[UnpackFunc](
      (0x01 | Format.MP_FIXARRAY).toByte -> { in: DIS =>
        val r1 = codec1.unpack(in)
        apply(r1)
      }
    )
  }


  class CaseClassCodec2[T, A1, A2](
    apply: (A1, A2) => T,
    unapply: T => Option[(A1, A2)]
  )(
    implicit K1: Codec[A1],
    K2: Codec[A2]
  ) extends Codec[T] {
    val codec = new TupleCodec2[A1, A2]
    val _apply: ((A1, A2)) => T = Function.tupled(apply)

    override def pack(out: DataOutputStream, item: T): Unit = {
      codec.pack(out, unapply(item).get)
    }
    val unpackFuncMap = codec.unpackFuncMap.mapValues(_.andThen(_apply))
  }

  class CaseClassCodec3[T, A1, A2, A3](
    apply: (A1, A2, A3) => T,
    unapply: T => Option[(A1, A2, A3)]
  )(
    implicit K1: Codec[A1],
    K2: Codec[A2],
    K3: Codec[A3]
  ) extends Codec[T] {
    val codec = new TupleCodec3[A1, A2, A3]
    val _apply = Function.tupled(apply)

    override def pack(out: DataOutputStream, item: T): Unit = {
      codec.pack(out, unapply(item).get)
    }
    val unpackFuncMap = codec.unpackFuncMap.mapValues(_.andThen(_apply))
  }

  class CaseClassCodec4[T, A1, A2, A3, A4](
    apply: (A1, A2, A3, A4) => T,
    unapply: T => Option[(A1, A2, A3, A4)]
  )(
    implicit K1: Codec[A1],
    K2: Codec[A2],
    K3: Codec[A3],
    K4: Codec[A4]
  ) extends Codec[T] {
    val codec = new TupleCodec4[A1, A2, A3, A4]
    val _apply = Function.tupled(apply)

    override def pack(out: DataOutputStream, item: T): Unit = {
      codec.pack(out, unapply(item).get)
    }
    val unpackFuncMap = codec.unpackFuncMap.mapValues(_.andThen(_apply))
  }
}
