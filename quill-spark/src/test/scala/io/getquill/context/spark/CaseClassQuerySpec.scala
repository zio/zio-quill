package io.getquill.context.spark

import io.getquill.Spec
import org.apache.spark.sql.Dataset
import org.scalatest.Matchers._

case class Contact(firstName: String, lastName: String, age: Int, addressFk: Int, extraInfo: String)
case class Address(id: Int, street: String, zip: Int, otherExtraInfo: String)
case class AddressableContact(firstName: String, lastName: String, age: Int, street: String, zip: Int)

case class ContactSimplified(firstName: String, lastNameRenamed: String, firstReverse: String)
case class ContactSimplifiedMapped(firstNameMapped: String, lastNameMapped: String, firstReverseMapped: String)

class CaseClassQuerySpec extends Spec {

  val context = io.getquill.context.sql.testContext

  val expectedData = Seq(
    ContactSimplified("Alex", "Jones", "Alex".reverse),
    ContactSimplified("Bert", "James", "Bert".reverse),
    ContactSimplified("Cora", "Jasper", "Cora".reverse)
  )

  import testContext._
  import sqlContext.implicits._

  val peopleEntries = liftQuery {
    Seq(
      Contact("Alex", "Jones", 60, 2, "foo"),
      Contact("Bert", "James", 55, 3, "bar"),
      Contact("Cora", "Jasper", 33, 3, "baz")
    ).toDS()
  }

  val addressEntries = liftQuery {
    Seq(
      Address(1, "123 Fake Street", 11234, "something"),
      Address(2, "456 Old Street", 45678, "something else"),
      Address(3, "789 New Street", 89010, "another thing")
    ).toDS()
  }

  val reverse = quote {
    (str: String) => infix"reverse(${str})".as[String]
  }

  "Simple Join" in {
    val q = quote {
      for {
        p <- peopleEntries
        a <- addressEntries if p.addressFk == a.id
      } yield {
        new AddressableContact(p.firstName, p.lastName, p.age, a.street, a.zip)
      }
    }

    testContext.run(q).collect() should contain theSameElementsAs Seq(
      AddressableContact("Alex", "Jones", 60, "456 Old Street", 45678),
      AddressableContact("Bert", "James", 55, "789 New Street", 89010),
      AddressableContact("Cora", "Jasper", 33, "789 New Street", 89010)
    )
  }

  "Simple Join - External Map" in {
    val q = quote {
      for {
        p <- peopleEntries
        a <- addressEntries if p.addressFk == a.id
      } yield {
        AddressableContact(p.firstName, p.lastName, p.age, a.street, a.zip)
      }
    }

    val dataset: Dataset[AddressableContact] = testContext.run(q)
    val mapped = dataset.map(ac => ContactSimplified(ac.firstName, ac.lastName, ac.firstName.reverse))

    mapped.collect() should contain theSameElementsAs expectedData

  }

  "Simple Select" in {
    val q = quote {
      for {
        p <- peopleEntries
      } yield ContactSimplified(p.firstName, p.lastName, reverse(p.firstName))
    }
    testContext.run(q).collect() should contain theSameElementsAs expectedData
  }

  "Two Level Select" in {
    val q = quote {
      for {
        p <- peopleEntries
      } yield ContactSimplified(p.firstName, p.lastName, reverse(p.firstName))
    }

    val q2 = quote {
      for {
        p <- q
      } yield ContactSimplified(p.firstName, p.lastNameRenamed, reverse(p.firstName))
    }
    testContext.run(q2).collect() should contain theSameElementsAs expectedData
  }

  "Two Level Select - Filtered First Part" in {
    val q = quote {
      for {
        p <- peopleEntries if (p.firstName == "Bert")
      } yield ContactSimplified(p.firstName, p.lastName, reverse(p.firstName))
    }

    val q2 = quote {
      for {
        p <- q
      } yield ContactSimplified(p.firstName, p.lastNameRenamed, reverse(p.firstName))
    }
    testContext.run(q2).collect() should contain theSameElementsAs expectedData.filter(_.firstName == "Bert")
  }

  "Two Level Select - Filtered Second Part" in {
    val q = quote {
      for {
        p <- peopleEntries
      } yield ContactSimplified(p.firstName, p.lastName, reverse(p.firstName))
    }

    val q2 = quote {
      for {
        p <- q if (p.lastNameRenamed == "James")
      } yield ContactSimplified(p.firstName, p.lastNameRenamed, reverse(p.firstName))
    }
    testContext.run(q2).collect() should contain theSameElementsAs expectedData.filter(_.firstName == "Bert")
  }

  "Two Level Select - Filtered First and Second Part" in {
    val q = quote {
      for {
        p <- peopleEntries if (p.firstName == "Bert" || p.firstName == "Alex")
      } yield ContactSimplified(p.firstName, p.lastName, reverse(p.firstName))
    }

    val q2 = quote {
      for {
        p <- q if (p.lastNameRenamed == "James")
      } yield ContactSimplified(p.firstName, p.lastNameRenamed, reverse(p.firstName))
    }
    testContext.run(q2).collect() should contain theSameElementsAs expectedData.filter(_.firstName == "Bert")
  }

  "Two Level Select Tuple" in {
    val q = quote {
      for {
        p <- peopleEntries
      } yield (p.firstName, p.lastName, reverse(p.firstName))
    }

    val q2 = quote {
      for {
        p <- q
      } yield ContactSimplifiedMapped(p._1, p._2, reverse(p._1))
    }

    testContext.run(q2).collect() should contain theSameElementsAs expectedData.map(
      c => ContactSimplifiedMapped(c.firstName, c.lastNameRenamed, c.firstReverse)
    )
  }

}
