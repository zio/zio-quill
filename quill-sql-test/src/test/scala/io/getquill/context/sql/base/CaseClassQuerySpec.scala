package io.getquill.context.sql.base

import io.getquill.base.Spec
import io.getquill.context.sql.SqlContext

trait CaseClassQuerySpec extends Spec {

  val context: SqlContext[_, _]

  import context._

  case class Contact(firstName: String, lastName: String, age: Int, addressFk: Int, extraInfo: String)

  case class Address(id: Int, street: String, zip: Int, otherExtraInfo: String)

  case class Nickname(nickname: String)

  case class NicknameSameField(firstName: String)

  val peopleInsert =
    quote((p: Contact) => query[Contact].insertValue(p))

  val peopleEntries = List(
    Contact("Alex", "Jones", 60, 2, "foo"),
    Contact("Bert", "James", 55, 3, "bar"),
    Contact("Cora", "Jasper", 33, 3, "baz")
  )

  val addressInsert =
    quote((c: Address) => query[Address].insertValue(c))

  val addressEntries = List(
    Address(1, "123 Fake Street", 11234, "something"),
    Address(2, "456 Old Street", 45678, "something else"),
    Address(3, "789 New Street", 89010, "another thing")
  )

  case class ContactSimplified(firstName: String, lastName: String, age: Int)

  case class AddressableContact(firstName: String, lastName: String, age: Int, street: String, zip: Int)

  val `Ex 1 CaseClass Record Output` = quote {
    query[Contact].map(p => new ContactSimplified(p.firstName, p.lastName, p.age))
  }

  val `Ex 1A CaseClass Record Output` = quote {
    query[Contact].map(p => ContactSimplified(p.firstName, p.lastName, p.age))
  }

  val `Ex 1B CaseClass Record Output` = quote {
    query[Contact].map(p => ContactSimplified.apply(p.firstName, p.lastName, p.age))
  }

  val `Ex 1 CaseClass Record Output expected result` = List(
    ContactSimplified("Alex", "Jones", 60),
    ContactSimplified("Bert", "James", 55),
    ContactSimplified("Cora", "Jasper", 33)
  )

  val `Ex 2 Single-Record Join` =
    quote {
      for {
        p <- query[Contact]
        a <- query[Address] if p.addressFk == a.id
      } yield {
        new AddressableContact(p.firstName, p.lastName, p.age, a.street, a.zip)
      }
    }
  val `Ex 2 Single-Record Join expected result` = List(
    AddressableContact("Alex", "Jones", 60, "456 Old Street", 45678),
    AddressableContact("Bert", "James", 55, "789 New Street", 89010),
    AddressableContact("Cora", "Jasper", 33, "789 New Street", 89010)
  )

  val `Ex 3 Inline Record Usage` = quote {
    val person = new ContactSimplified("Alex", "Jones", 44)
    query[Contact].filter(p => p.firstName == person.firstName && person.lastName == person.lastName)
  }

  val `Ex 3 Inline Record Usage expected result` = List(
    new Contact("Alex", "Jones", 60, 2, "foo")
  )

  val `Ex 4 Mapped Union of Nicknames` = quote {
    query[Contact].map(c => Nickname(c.firstName)) union query[Contact].map(c => Nickname(c.firstName))
  }

  val `Ex 4 Mapped Union All of Nicknames` = quote {
    query[Contact].map(c => Nickname(c.firstName)) ++ query[Contact].map(c => Nickname(c.firstName))
  }

  val `Ex 4 Mapped Union All of Nicknames Filtered` = quote {
    `Ex 4 Mapped Union All of Nicknames`.filter(_.nickname == "Alex")
  }

  val `Ex 4 Mapped Union All of Nicknames Same Field` = quote {
    query[Contact].map(c => NicknameSameField(c.firstName)) ++ query[Contact].map(c => NicknameSameField(c.firstName))
  }

  val `Ex 4 Mapped Union All of Nicknames Same Field Filtered` = quote {
    `Ex 4 Mapped Union All of Nicknames Same Field`.filter(_.firstName == "Alex")
  }

  val `Ex 4 Mapped Union of Nicknames expected result` =
    List(Nickname("Alex"), Nickname("Bert"), Nickname("Cora"))

  val `Ex 4 Mapped Union All of Nicknames expected result` =
    List(Nickname("Alex"), Nickname("Bert"), Nickname("Cora"), Nickname("Alex"), Nickname("Bert"), Nickname("Cora"))

  val `Ex 4 Mapped Union All of Nicknames Same Field expected result` =
    List(
      NicknameSameField("Alex"),
      NicknameSameField("Bert"),
      NicknameSameField("Cora"),
      NicknameSameField("Alex"),
      NicknameSameField("Bert"),
      NicknameSameField("Cora")
    )

  val `Ex 4 Mapped Union All of Nicknames Filtered expected result` =
    List(
      Nickname("Alex"),
      Nickname("Alex")
    )

  val `Ex 4 Mapped Union All of Nicknames Same Field Filtered expected result` =
    List(
      NicknameSameField("Alex"),
      NicknameSameField("Alex")
    )
}
