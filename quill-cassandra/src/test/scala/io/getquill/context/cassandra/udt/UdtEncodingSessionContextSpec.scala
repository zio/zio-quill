package io.getquill.context.cassandra.udt

import io.getquill.{ CassandraContextConfig, CassandraSyncContext, SnakeCase }
import io.getquill.context.cassandra.testSyncDB
import io.getquill.util.LoadConfig
import io.getquill.Udt

class UdtEncodingSessionContextSpec extends UdtSpec {

  val ctx1 = testSyncDB
  val cluster = CassandraContextConfig(LoadConfig("testSyncDB")).cluster
  val ctx2 = new CassandraSyncContext(SnakeCase, cluster, "quill_test_2", 1000)

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
      ctx1.run(query[WithEverything].insert(lift(e)))
      ctx1.run(query[WithEverything].filter(_.id == 1)).headOption must contain(e)
    }
    "with meta" in {
      case class MyName(first: String) extends Udt
      case class WithEverything(id: Int, name: MyName, nameList: List[MyName])
      implicit val myNameMeta = udtMeta[MyName]("Name", _.first -> "firstName")

      val e = WithEverything(2, MyName("first"), List(MyName("first")))
      ctx1.run(query[WithEverything].insert(lift(e)))
      ctx1.run(query[WithEverything].filter(_.id == 2)).headOption must contain(e)
    }
  }

  "fail on inconsistent states" - {
    val ctx0 = new CassandraSyncContext(SnakeCase, cluster, null, 1000)
    "found several UDT with the same name, but not in current session" in {
      intercept[IllegalStateException](ctx0.udtValueOf("Name")).getMessage mustBe
        "Could not determine to which keyspace `Name` UDT belongs. Please specify desired keyspace using UdtMeta"

      // but ok when specified
      ctx0.udtValueOf("Name", Some("quill_test")).getType.getKeyspace mustBe "quill_test"
      // "nAmE" - identifiers are case insensitive
      ctx0.udtValueOf("nAmE", Some("quill_test_2")).getType.getKeyspace mustBe "quill_test_2"
    }
    "could not find UDT with given name" in {
      intercept[IllegalStateException](ctx0.udtValueOf("Whatever")).getMessage mustBe
        "Could not find UDT `Whatever` in any keyspace"
    }
  }

  "return udt if it's found in another keyspace" in {
    ctx2.udtValueOf("Personal").getType.getKeyspace mustBe "quill_test"
  }

  "naming strategy" in {
    import ctx2._
    case class WithUdt(id: Int, name: Name)
    val e = WithUdt(1, Name("first", Some("second")))
    // quill_test_2 uses snake case
    ctx2.run(query[WithUdt].insert(lift(e)))
    ctx2.run(query[WithUdt].filter(_.id == 1)).headOption must contain(e)
  }

  override protected def beforeAll(): Unit = {
    clean1()
    clean2()
  }

  private def clean1(): Unit = {
    import ctx1._
    ctx1.run(querySchema[Name]("WithEverything").delete)
    ()
  }

  private def clean2(): Unit = {
    import ctx2._
    ctx2.run(querySchema[Name]("with_udt").delete)
    ()
  }

  override protected def afterAll(): Unit = {
    ctx2.close()
  }
}
