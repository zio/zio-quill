package io.getquill.context.cassandra.udt

import io.getquill.context.cassandra.mirrorContext

class UdtEncodingMirrorContextSpec extends UdtSpec {

  import mirrorContext._

  "Provide implicit decoders/encoders" - {

    "UDT raw columns" in {
      implicitly[Decoder[Name]]
      implicitly[Encoder[Name]]
    }

    "UDT collections columns" in {
      implicitly[Decoder[List[Name]]]
      implicitly[Encoder[List[Name]]]

      implicitly[Decoder[Set[Name]]]
      implicitly[Encoder[Set[Name]]]

      implicitly[Decoder[Map[String, Name]]]
      implicitly[Encoder[Map[Name, String]]]
    }
  }

  "Encode/decode UDT within entity" in {
    case class User(id: Int, name: Name, names: List[Name])
    mirrorContext.run(query[User]).string mustBe "SELECT id, name, names FROM User"
    mirrorContext.run(query[User]
      .insert(lift(User(1, Name("1", None), Nil)))).string mustBe "INSERT INTO User (id,name,names) VALUES (?, ?, ?)"
  }
}
