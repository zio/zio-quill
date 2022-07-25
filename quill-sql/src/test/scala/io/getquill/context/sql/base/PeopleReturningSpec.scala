package io.getquill.context.sql.base

import io.getquill.base.Spec
import io.getquill.context.sql.SqlContext
import org.scalatest.BeforeAndAfterEach

trait PeopleReturningSpec extends Spec with BeforeAndAfterEach {

  val context: SqlContext[_, _]

  import context._

  case class Contact(firstName: String, lastName: String, age: Int, addressFk: Int = 0, extraInfo: Option[String] = None)

  case class Product(id: Long, description: String, sku: Int)

  val peopleInsert =
    quote((p: Contact) => query[Contact].insertValue(p))

  val people = List(
    Contact("Joe", "A", 44),
    Contact("Dan", "D", 55),
    Contact("Joe", "B", 66),
    Contact("Jim", "J", 77)
  )

  val product = Product(0, "Something", 123)

  object `Ex 0 insert.returning(_.generatedColumn) mod` {
    val op = quote {
      query[Product].insert(_.description -> lift(product.description), _.sku -> lift(product.sku)).returning(p => p.id)
    }
    val get = quote {
      query[Product]
    }

    def result(id: Long) = List(product.copy(id = id))
  }

  object `Ex 0.5 insert.returning(wholeRecord) mod` {
    val op = quote {
      query[Product].insert(_.description -> lift(product.description), _.sku -> lift(product.sku)).returning(p => p)
    }
    val get = quote {
      query[Product]
    }

    def result(newProduct: Product) = List(newProduct)
  }

  object `Ex 1 insert.returningMany(_.generatedColumn) mod` {
    val op = quote {
      query[Product].insert(_.description -> lift(product.description), _.sku -> lift(product.sku)).returningMany(p => p.id)
    }
    val get = quote {
      query[Product]
    }

    def result(id: Long) = List(product.copy(id = id))
  }

  object `Ex 2 update.returningMany(_.singleColumn) mod` {
    val op = quote {
      query[Contact].filter(p => p.firstName == "Joe").update(p => p.age -> (p.age + 1)).returningMany(p => p.lastName)
    }
    val expect = people.filter(_.firstName == "Joe").map(_.lastName)
    val get = quote {
      query[Contact]
    }
    val result = people.map(p => if (p.firstName == "Joe") p.copy(age = p.age + 1) else p)
  }

  object `Ex 3 delete.returningMany(wholeRecord)` {
    val op = quote {
      query[Contact].filter(p => p.firstName == "Joe").delete.returningMany(p => p)
    }
    val expect = people.filter(p => p.firstName == "Joe")
    val get = quote {
      query[Contact]
    }
    val result = people.filterNot(p => p.firstName == "Joe")
  }

  object `Ex 4 update.returningMany(query)` {
    val op = quote {
      query[Contact]
        .filter(p => p.firstName == "Joe")
        .update(p => p.age -> (p.age + 1))
        .returningMany(p => query[Contact].filter(cp => cp.firstName == p.firstName && cp.lastName == p.lastName).map(_.lastName).value.orNull)
    }
    val expect = List("A", "B")
    val get = quote {
      query[Contact]
    }
    val result = people.map(p => if (p.firstName == "Joe") p.copy(age = p.age + 1) else p)
  }
}
