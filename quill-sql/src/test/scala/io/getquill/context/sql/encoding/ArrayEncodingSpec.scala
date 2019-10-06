package io.getquill.context.sql.encoding

import io.getquill.Spec
import io.getquill.context.sql.{ testContext => ctx }

class ArrayEncodingSpec extends Spec {
  import ctx._

  case class Raw()
  case class Decor(raw: Raw)

  object impl {
    implicit def encodeRaw[Col <: Seq[Raw]]: Encoder[Col] = encoder[Col]
    implicit def decodeRaw[Col <: Seq[Raw]]: Decoder[Col] = decoderUnsafe[Col]
    implicit val encodeDecor: MappedEncoding[Decor, Raw] = MappedEncoding(_.raw)
    implicit val decodeDecor: MappedEncoding[Raw, Decor] = MappedEncoding(Decor.apply)
  }

  "Provide array support with MappingEncoding" - {
    "encoders" in {
      import impl.{ encodeRaw, encodeDecor }
      implicitly[Encoder[List[Decor]]]
    }
    "decoders" in {
      import impl.{ decodeRaw, decodeDecor }
      implicitly[Decoder[Vector[Decor]]]
    }
  }

  "Do not compile in case of missing MappedEncoding" - {
    "encoders" in {
      import impl.encodeRaw
      "implicitly[Encoder[List[Decor]]]" mustNot compile
    }
    "decoders" in {
      import impl.decodeRaw
      "implicitly[Decoder[Vector[Decor]]]" mustNot compile
    }
  }

  "Do not compile in case of missing base encoding" - {
    "encoders" in {
      import impl.encodeDecor
      "implicitly[Encoder[List[Decor]]]" mustNot compile
    }
    "decoders" in {
      import impl.decodeDecor
      "implicitly[Decoder[Vector[Decor]]]" mustNot compile
    }
  }

  "Do not compile in case of upper bound types to Seq or not applicable types" - {
    "encoders" in {
      import impl.encodeDecor
      implicit val e = encoder[Iterable[Raw]]
      "implicitly[Encoder[List[Decor]]]" mustNot compile
    }
    "decoders" in {
      import impl.decodeDecor
      implicit val d = decoder[Set[Raw]]
      "implicitly[Decoder[Vector[Decor]]]" mustNot compile
    }
  }
}
