package io.getquill.context.sql

import io.getquill.Spec

trait DistinctSpec extends Spec {

  val context: SqlContext[_, _]

  import context._

  case class Person(name: String, age: Int)
  case class Couple(him: String, her: String)

  val peopleInsert =
    quote((p: Person) => query[Person].insert(p))

  val peopleEntries = List(
    Person("A", 1),
    Person("B", 2),
    Person("X", 10),
    Person("Y", 11),
    Person("Z", 12)
  )

  val couplesInsert =
    quote((c: Couple) => query[Couple].insert(c))

  val couplesEntries = List(
    Couple("B", "X"),
    Couple("B", "Y"),
    Couple("B", "Z"),
    Couple("A", "X"),
    Couple("A", "X"),
    Couple("A", "Y")
  )

  val `Ex 1 Distinct One Field` = quote {
    query[Couple].map(_.him).distinct
  }
  val `Ex 1 Distinct One Field Result` =
    List("B", "A")

  val `Ex 2 Distinct Two Field Tuple` = quote {
    query[Couple].map(c => (c.him, c.her)).distinct
  }
  val `Ex 2 Distinct Two Field Tuple Result` =
    List(
      ("B", "X"),
      ("B", "Y"),
      ("B", "Z"),
      ("A", "X"),
      ("A", "Y")
    )

  val `Ex 2a Distinct Two Field Tuple Same Element` = quote {
    query[Couple].map(c => (c.him, c.him)).distinct
  }
  val `Ex 2a Distinct Two Field Tuple Same Element Result` =
    List(
      ("B", "B"),
      ("A", "A")
    )

  val `Ex 3 Distinct Two Field Case Class` = quote {
    query[Couple].distinct
  }
  val `Ex 3 Distinct Two Field Case Class Result` =
    List(
      Couple("B", "X"),
      Couple("B", "Y"),
      Couple("B", "Z"),
      Couple("A", "X"),
      Couple("A", "Y")
    )

  val `Ex 4-base non-Distinct Subquery` = quote {
    query[Person].join(query[Couple]).on(_.name == _.him)
  }
  val `Ex 4-base non-Distinct Subquery Result` =
    List(
      (Person("A", 1), Couple("A", "X")),
      (Person("A", 1), Couple("A", "X")),
      (Person("A", 1), Couple("A", "Y")),
      (Person("B", 2), Couple("B", "X")),
      (Person("B", 2), Couple("B", "Y")),
      (Person("B", 2), Couple("B", "Z"))
    )

  val `Ex 4 Distinct Subquery` = quote {
    query[Person].join(query[Couple].distinct).on(_.name == _.him)
  }
  val `Ex 4 Distinct Subquery Result` =
    List(
      (Person("A", 1), Couple("A", "X")),
      (Person("A", 1), Couple("A", "Y")),
      (Person("B", 2), Couple("B", "X")),
      (Person("B", 2), Couple("B", "Y")),
      (Person("B", 2), Couple("B", "Z"))
    )

  val `Ex 5 Distinct Subquery with Map Single Field` = quote {
    query[Person]
      .join(
        query[Couple].map(_.him).distinct
      ).on((p, cm) => p.name == cm)
  }
  val `Ex 5 Distinct Subquery with Map Single Field Result` =
    List(
      (Person("A", 1), "A"),
      (Person("B", 2), "B")
    )

  val `Ex 6 Distinct Subquery with Map Multi Field` = quote {
    query[Person]
      .join(
        query[Couple].map(c => (c.him, c.her)).distinct
      ).on(_.name == _._1)
  }
  val `Ex 6 Distinct Subquery with Map Multi Field Result` =
    List(
      (Person("A", 1), ("A", "X")),
      (Person("A", 1), ("A", "Y")),
      (Person("B", 2), ("B", "X")),
      (Person("B", 2), ("B", "Y")),
      (Person("B", 2), ("B", "Z"))
    )

  case class TwoField(one: String, two: String)
  val `Ex 7 Distinct Subquery with Map Multi Field Tuple` = quote {
    query[Person]
      .join(
        query[Couple].map(c => TwoField(c.him, c.her)).distinct
      ).on(_.name == _.one)
  }
  val `Ex 7 Distinct Subquery with Map Multi Field Tuple Result` =
    List(
      (Person("A", 1), TwoField("A", "X")),
      (Person("A", 1), TwoField("A", "Y")),
      (Person("B", 2), TwoField("B", "X")),
      (Person("B", 2), TwoField("B", "Y")),
      (Person("B", 2), TwoField("B", "Z"))
    )

  val `Ex 8 Distinct With Sort` = quote {
    query[Person]
      .join(query[Couple]).on(_.name == _.him)
      .distinct
      .sortBy(_._1.name)(Ord.asc)
  }
  val `Ex 8 Distinct With Sort Result` =
    List(
      (Person("A", 1), Couple("A", "X")),
      (Person("A", 1), Couple("A", "Y")),
      (Person("B", 2), Couple("B", "X")),
      (Person("B", 2), Couple("B", "Y")),
      (Person("B", 2), Couple("B", "Z"))
    )
}
