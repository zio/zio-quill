package io.getquill.context.sql

import io.getquill.Spec

trait OptionQuerySpec extends Spec {

  val context: SqlContext[_, _]

  import context._

  case class NoAddressContact(firstName: String, lastName: String, age: Int)
  case class HasAddressContact(firstName: String, lastName: String, age: Int, addressFk: Int)
  case class Contact(firstName: String, lastName: String, age: Int, addressFk: Option[Int], extraInfo: String)
  case class Address(id: Int, street: String, zip: Int, otherExtraInfo: Option[String])

  val peopleInsert =
    quote((p: Contact) => query[Contact].insert(p))

  val peopleEntries = List(
    Contact("Alex", "Jones", 60, Option(1), "foo"),
    Contact("Bert", "James", 55, Option(2), "bar"),
    Contact("Cora", "Jasper", 33, None, "baz")
  )

  val addressInsert =
    quote((c: Address) => query[Address].insert(c))

  val addressEntries = List(
    Address(1, "123 Fake Street", 11234, Some("something")),
    Address(2, "456 Old Street", 45678, Some("something else")),
    Address(3, "789 New Street", 89010, None),
    Address(111, "111 Default Address", 12345, None)
  )

  val `Simple Map with Condition` = quote {
    query[Address].map(a => (a.street, a.otherExtraInfo.map(info => if (info == "something") "one" else "two")))
  }
  val `Simple Map with Condition Result` = List(
    ("123 Fake Street", Some("one")),
    ("456 Old Street", Some("two")),
    ("789 New Street", None),
    ("111 Default Address", None)
  )

  val `Simple Map with GetOrElse` = quote {
    query[Address].map(
      a => (a.street, a.otherExtraInfo.map(info => info + " suffix").getOrElse("baz"))
    )
  }
  val `Simple Map with GetOrElse Result` = List(
    ("123 Fake Street", "something suffix"),
    ("456 Old Street", "something else suffix"),
    ("789 New Street", "baz"),
    ("111 Default Address", "baz")
  )

  val `Simple Map with Condition and GetOrElse` = quote {
    query[Address].map(
      a => (a.street, a.otherExtraInfo.map(info => if (info == "something") "foo" else "bar").getOrElse("baz"))
    )
  }
  val `Simple Map with Condition and GetOrElse Result` = List(
    ("123 Fake Street", "foo"),
    ("456 Old Street", "bar"),
    ("789 New Street", "baz"),
    ("111 Default Address", "baz")
  )

  val `Simple GetOrElse` = quote {
    query[Address].map(a => (a.street, a.otherExtraInfo.getOrElse("yet something else")))
  }
  val `Simple GetOrElse Result` = List(
    ("123 Fake Street", "something"),
    ("456 Old Street", "something else"),
    ("789 New Street", "yet something else"),
    ("111 Default Address", "yet something else")
  )

  val `LeftJoin with FlatMap` = quote {
    query[Contact].leftJoin(query[Address]).on((c, a) => c.addressFk.exists(_ == a.id))
      .map({ case (c, a) => (a.map(_.id), a.flatMap(_.otherExtraInfo)) })
  }
  val `LeftJoin with FlatMap Result` = List(
    (Some(1), Some("something")),
    (Some(2), Some("something else")),
    (None, None)
  )

  val `LeftJoin with Flatten` = quote {
    query[Contact].leftJoin(query[Address]).on((c, a) => c.addressFk.exists(_ == a.id))
      .map({ case (c, a) => (a.map(_.id), a.map(_.otherExtraInfo).flatten) })
  }
  val `LeftJoin with Flatten Result` = List(
    (Some(1), Some("something")),
    (Some(2), Some("something else")),
    (None, None)
  )

  val `Map+getOrElse LeftJoin` = quote {
    query[Contact].leftJoin(query[Address]).on((c, a) => c.addressFk.getOrElse(-1) == a.id)
      .map({ case (c, a) => (a.map(_.id), a.flatMap(_.otherExtraInfo)) })
  }
  val `Map+getOrElse LeftJoin Result` = List(
    (Some(1), Some("something")),
    (Some(2), Some("something else")),
    (None, None)
  )

  case class NormalizedContact(name: String, addressFk: Option[Int])

  def normalizeAddress = quote {
    (addressFk: Option[Int]) => addressFk.getOrElse(111)
  }

  val `Option+Some+None Normalize` = quote {
    val c1 = querySchema[NoAddressContact]("Contact").map(c => (c.firstName, None: Option[Int]))
    val c2 = querySchema[HasAddressContact]("Contact").map(c => (c.firstName, Some(c.addressFk)))
    val c3 = query[Contact].map(c => (c.firstName, c.addressFk))

    val normalized = (c1 ++ c2 ++ c3).map({ case (name, address) => (name, normalizeAddress(address)) })

    for {
      (name, addressFk) <- normalized
      address <- query[Address] if address.id == addressFk
    } yield (name, address.street)
  }

  val `Option+Some+None Normalize Result` = List(
    ("Alex", "111 Default Address"),
    ("Bert", "111 Default Address"),
    ("Cora", "111 Default Address"),
    ("Alex", "123 Fake Street"),
    ("Bert", "456 Old Street"),
    ("Cora", "111 Default Address"),
    ("Alex", "123 Fake Street"),
    ("Bert", "456 Old Street"),
    ("Cora", "111 Default Address")
  )

}
