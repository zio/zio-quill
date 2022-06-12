package io.getquill

import io.getquill.context.mirror.{ MirrorSession, Row }

class OptionalProductEncodingSpec extends Spec {

  val ctx = new MirrorContext(PostgresDialect, Literal)
  import ctx._

  "optional product with optional embedded row" in {
    case class Name(first: String, last: Int) extends Embedded
    case class Person(id: Int, name: Option[Name], age: Int)
    case class Address(owner: Int, street: String)

    val result =
      ctx.run {
        for {
          p <- query[Address]
          a <- query[Person].leftJoin(a => a.id == p.owner)
        } yield (p, a)
      }

    // Given:
    // Row(a.owner, a.street, p.id, p.name.first, p.name.last, p.age)

    // Row(123    , "St"    , 123 , null, null, 44) => (Address(123, "st"), Some(Person(123, None, 44)))
    result.extractor(Row(123, "St", 123, null, null, 44), MirrorSession.default) mustEqual
      (((Address(123, "St"), Some(Person(123, None, 44)))))

    result.extractor(Row(123, "St", null, null, null, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), None))

    result.extractor(Row(123, "St", 123, null, null, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), Some(Person(123, None, 0))))

    // Row(123    , "St"    , 123 , "Joe", null, null) => (Address(123, "st"), Some(Person(123, Name("Joe", 0), 0)))
    result.extractor(Row(123, "St", 123, "Joe", null, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), Some(Person(123, Some(Name("Joe", 0)), 0))))

    // Row(123    , "St"    , 123 , null, 1337, null) => (Address(123, "st"), Some(Person(123, Name(null, 1337), 0)))
    result.extractor(Row(123, "St", 123, null, 1337, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), Some(Person(123, Some(Name(null, 1337)), 0))))

  }

  "optional product with multiple nested optional embeds" in {
    case class InnerName(title: Int, last: String) extends Embedded
    case class Name(first: String, last: Option[InnerName]) extends Embedded
    case class Address(owner: Int, street: String)
    case class Person(id: Int, name: Option[Name])

    val result =
      ctx.run {
        for {
          p <- query[Address]
          a <- query[Person].leftJoin(a => a.id == p.owner)
        } yield (p, a)
      }

    result.extractor(Row(123, "St", 123, null, null, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), Some(Person(123, None))))

    result.extractor(Row(123, "St", 123, "Joe", null, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), Some(Person(123, Some(Name("Joe", None))))))

    result.extractor(Row(123, "St", 123, null, 1337, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), Some(Person(123, Some(Name(null, Some(InnerName(1337, null))))))))
  }

  "optional product with nested optional with optional leaf" in {
    case class Name(first: String, last: Option[Int]) extends Embedded
    case class Address(owner: Int, street: String)
    case class Person(id: Int, name: Option[Name], age: Int)

    val result =
      ctx.run {
        for {
          p <- query[Address]
          a <- query[Person].leftJoin(a => a.id == p.owner)
        } yield (p, a)
      }

    result.extractor(Row(123, "St", 123, "Joe", null, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), Some(Person(123, Some(Name("Joe", None)), 0))))

    result.extractor(Row(123, "St", 123, null, 1337, null), MirrorSession.default) mustEqual
      ((Address(123, "St"), Some(Person(123, Some(Name(null, Some(1337))), 0))))

  }

}
