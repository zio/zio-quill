package io.getquill.context.cassandra.alpakka.udt

import io.getquill.context.cassandra.alpakka.CassandraAlpakkaSpec
import io.getquill.context.cassandra.udt.UdtSpec
import io.getquill.Udt

class UdtEncodingSessionContextSpec extends UdtSpec with CassandraAlpakkaSpec {

  val ctx1 = testDB

  "Provide encoding for UDT" - {
    import ctx1._
    "raw" in {
      implicitly[Decoder[Name]]
      implicitly[Encoder[Name]]
    }
    "collections" in {
      implicitly[Decoder[List[Name]]]
      implicitly[Decoder[Set[Name]]]
      implicitly[Decoder[Map[String, Name]]]
      implicitly[Encoder[List[Name]]]
      implicitly[Encoder[Set[Name]]]
      implicitly[Encoder[Map[String, Name]]]
    }
    "nested" in {
      implicitly[Decoder[Personal]]
      implicitly[Encoder[Personal]]
      implicitly[Decoder[List[Personal]]]
      implicitly[Encoder[List[Personal]]]
    }
    "MappedEncoding" in {
      case class FirstName(name: String)
      case class MyName(firstName: FirstName) extends Udt

      implicit val encodeFirstName = MappedEncoding[FirstName, String](_.name)
      implicit val decodeFirstName = MappedEncoding[String, FirstName](FirstName)

      implicitly[Encoder[MyName]]
      implicitly[Decoder[MyName]]
      implicitly[Encoder[List[MyName]]]
      implicitly[Decoder[List[MyName]]]
    }
  }

  "Complete examples" - {
    import ctx1._
    "without meta" in {
      case class WithEverything(id: Int, personal: Personal, nameList: List[Name])

      val e = WithEverything(1, Personal(1, "strt",
        Name("first", Some("last")),
        Some(Name("f", None)),
        List("e"),
        Set(1, 2),
        Map(1 -> "1", 2 -> "2")),
        List(Name("first", None)))

      await {
        for {
          _ <- ctx1.run(query[WithEverything].insert(lift(e)))
          res <- ctx1.run(query[WithEverything].filter(_.id == 1))
        } yield {
          res.headOption must contain(e)
        }
      }
    }
    "with meta" in {
      case class MyName(first: String) extends Udt
      case class WithEverything(id: Int, name: MyName, nameList: List[MyName])
      implicit val myNameMeta = udtMeta[MyName]("Name", _.first -> "firstName")

      val e = WithEverything(2, MyName("first"), List(MyName("first")))

      await {
        for {
          _ <- ctx1.run(query[WithEverything].insert(lift(e)))
          res <- ctx1.run(query[WithEverything].filter(_.id == 2))
        } yield {
          res.headOption must contain(e)
        }
      }
    }
  }

  override protected def beforeAll(): Unit = {
    import ctx1._
    await {
      ctx1.run(querySchema[Name]("WithEverything").delete)
    }
    ()
  }
}
