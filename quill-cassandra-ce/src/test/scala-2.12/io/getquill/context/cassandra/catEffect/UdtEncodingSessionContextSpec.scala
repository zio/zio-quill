package io.getquill.context.cassandra.catEffect

import io.getquill.Udt
import io.getquill.context.cassandra.udt.UdtSpec
import io.getquill.context.cassandra.catsEffect.testCeDB._
import io.getquill.context.cassandra.catsEffect.testCeDB
import cats.effect.unsafe.implicits.global

class UdtEncodingSessionContextSpec extends UdtSpec {

  val ctx = testCeDB
  import ctx._

  "Provide encoding for UDT" - {
    "raw" in {
      implicitly[Decoder[Name]]
      implicitly[Encoder[Name]]
    }
    "collections" in {
      4
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
    "without meta" in {
      case class WithEverything(id: Int, personal: Personal, nameList: List[Name])

      val e = WithEverything(1, Personal(1, "strt",
        Name("first", Some("last")),
        Some(Name("f", None)),
        List("e"),
        Set(1, 2),
        Map(1 -> "1", 2 -> "2")),
        List(Name("first", None)))
      ctx.run(query[WithEverything].insert(lift(e))).unsafeRunSync()
      ctx.run(query[WithEverything].filter(_.id == 1)).unsafeRunSync().headOption must contain(e)
    }
    "with meta" in {
      case class MyName(first: String) extends Udt
      case class WithEverything(id: Int, name: MyName, nameList: List[MyName])
      implicit val myNameMeta = udtMeta[MyName]("Name", _.first -> "firstName")

      val e = WithEverything(2, MyName("first"), List(MyName("first")))
      ctx.run(query[WithEverything].insert(lift(e))).unsafeRunSync()
      ctx.run(query[WithEverything].filter(_.id == 2)).unsafeRunSync().headOption must contain(e)
    }
  }

  override def beforeAll: Unit = {
    ctx.run(querySchema[Name]("WithEverything").delete).unsafeRunSync()
    super.beforeAll()
    ()
  }
}
