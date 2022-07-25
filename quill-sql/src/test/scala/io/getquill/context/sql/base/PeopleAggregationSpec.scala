package io.getquill.context.sql.base

import io.getquill.Spec
import io.getquill.context.sql.SqlContext

trait PeopleAggregationSpec extends Spec {

  val context: SqlContext[_, _]

  import context._

  case class Contact(firstName: String, lastName: String, age: Int, addressFk: Int, extraInfo: Option[String] = None)

  case class Address(id: Int, street: String, zip: Int = 0, otherExtraInfo: Option[String] = None)

  val people = List(
    Contact("Joe", "A", 20, 1),
    Contact("Dan", "D", 30, 2),
    Contact("Joe", "B", 40, 3),
    Contact("Jim", "J", 50, 4),
    Contact("Dan", "E", 60, 1)
  )

  val addresses = List(
    Address(1, "111 St"),
    Address(2, "222 Ct"),
    Address(3, "333 Sq"),
    Address(4, "444 Ave")
  )

  object `Ex 1 map(agg(c),agg(c))` {
    val get = quote {
      query[Contact].map(p => (max(p.firstName), min(p.age)))
    }
    val expect = List(("Joe", 20))
  }

  object `Ex 2 map(agg(c),agg(c)).filter(col)` {
    val get = quote {
      query[Contact].map(p => (max(p.firstName), min(p.age))).filter { case (name, _) => name == "Joe" }
    }
    val expect = List(("Joe", 20))
  }

  object `Ex 3 groupByMap(col)(col,agg(c))` {
    val get = quote {
      query[Contact].groupByMap(p => p.firstName)(p => (p.firstName, max(p.age)))
    }
    val expect = people.groupBy(_.firstName).map { case (name, people) => (name, people.map(_.age).max) }
  }

  object `Ex 4 groupByMap(col)(agg(c)).filter(agg)` {
    val get = quote {
      query[Contact].groupByMap(p => p.firstName)(p => max(p.age)).filter(a => a < 1000)
    }
    val expect = people.groupBy(_.firstName).map { case (_, people) => (people.map(_.age).max) }
  }

  object `Ex 5 map.groupByMap(col)(col,agg(c)).filter(agg)` {
    case class Name(first: String, last: String, age: Int)

    val get = quote {
      query[Contact].map(c => Name(c.firstName, c.lastName, c.age)).groupByMap(p => p.first)(p => (p.first, max(p.age))).filter(t => t._1 == "Joe")
    }
    val expect = people.groupBy(_.firstName).map { case (name, people) => (name, people.map(_.age).max) }.filter(_._1 == "Joe").toSet
  }

  object `Ex 6 flatMap.groupByMap.map` {
    val get = quote {
      for {
        a <- query[Address]
        p <- query[Contact].groupByMap(p => p.addressFk)(p => (p.addressFk, max(p.age))).join(p => p._1 == a.id)
      } yield (a, p)
    }
    val expect = {
      val addressMaxAges = people.groupBy(_.addressFk).map { case (address, q) => (address, q.map(_.age).max) }
      val addressList = addresses.map(a => (a.id, a)).toMap
      addressMaxAges
        .map { case (addressId, maxAge) => (addressList.get(addressId), (addressId, maxAge)) }
        .collect { case (Some(addr), tup) => (addr, tup) }
        .toSet
    }
  }
}
