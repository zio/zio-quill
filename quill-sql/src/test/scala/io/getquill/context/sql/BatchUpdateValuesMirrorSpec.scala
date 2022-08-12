package io.getquill.context.sql

import io.getquill.context.ExecutionType.Unknown
import io.getquill.context.sql.base.BatchUpdateValuesSpec
import io.getquill.{ Literal, PostgresDialect, SqlMirrorContext }
import io.getquill.context.sql.util.StringOps._

class BatchUpdateValuesMirrorSpec extends BatchUpdateValuesSpec {

  val context = new SqlMirrorContext(PostgresDialect, Literal)
  import context._

  "Ex 1 - Simple Contact" in {
    import `Ex 1 - Simple Contact`._
    context.run(update, 2).tripleBatchMulti mustEqual List(
      (
        "UPDATE Contact AS p SET firstName = ps.firstName1, lastName = ps.lastName, age = ps.age FROM (VALUES (?, ?, ?, ?), (?, ?, ?, ?)) AS ps(firstName, firstName1, lastName, age) WHERE p.firstName = ps.firstName",
        List(
          List("Joe", "Joe", "BloggsU", 22, "Jan", "Jan", "RoggsU", 33),
          List("James", "James", "JonesU", 44, "Dale", "Dale", "DomesU", 55)
        ),
          Unknown
      ), (
        "UPDATE Contact AS p SET firstName = ps.firstName1, lastName = ps.lastName, age = ps.age FROM (VALUES (?, ?, ?, ?)) AS ps(firstName, firstName1, lastName, age) WHERE p.firstName = ps.firstName",
        List(
          List("Caboose", "Caboose", "CastleU", 66)
        ),
          Unknown
      )
    )
  }

  "Ex 1.1 - Simple Contact With Lift" in {
    import `Ex 1.1 - Simple Contact With Lift`._
    context.run(update, 2).tripleBatchMulti mustEqual List(
      (
        "UPDATE Contact AS p SET firstName = ps.firstName1, lastName = ps.lastName, age = ps.age FROM (VALUES (?, ?, ?, ?), (?, ?, ?, ?)) AS ps(firstName, firstName1, lastName, age) WHERE p.firstName = ps.firstName AND p.firstName = ?",
        List(
          List("Joe", "Joe", "BloggsU", 22, "Jan", "Jan", "RoggsU", 33, "Joe"),
          List("James", "James", "JonesU", 44, "Dale", "Dale", "DomesU", 55, "Joe")
        ), Unknown
      ), (
        "UPDATE Contact AS p SET firstName = ps.firstName1, lastName = ps.lastName, age = ps.age FROM (VALUES (?, ?, ?, ?)) AS ps(firstName, firstName1, lastName, age) WHERE p.firstName = ps.firstName AND p.firstName = ?",
        List(
          List("Caboose", "Caboose", "CastleU", 66, "Joe")
        ), Unknown
      )
    )
  }

  "Ex 1.2 - Simple Contact With 2 Lifts" in {
    import `Ex 1.2 - Simple Contact Mixed Lifts`._
    context.run(update, 2).tripleBatchMulti mustEqual List(
      (
        "UPDATE Contact AS p SET lastName = ps.lastName || ? FROM (VALUES (?, ?), (?, ?)) AS ps(firstName, lastName) WHERE p.firstName = ps.firstName AND (p.firstName = ? OR p.firstName = ?)",
        List(List(" Jr.", "Joe", "BloggsU", "Jan", "RoggsU", "Joe", "Jan"), List(" Jr.", "James", "JonesU", "Dale", "DomesU", "Joe", "Jan")), Unknown
      ), (
        "UPDATE Contact AS p SET lastName = ps.lastName || ? FROM (VALUES (?, ?)) AS ps(firstName, lastName) WHERE p.firstName = ps.firstName AND (p.firstName = ? OR p.firstName = ?)",
        List(List(" Jr.", "Caboose", "CastleU", "Joe", "Jan")), Unknown
      )
    )
  }

  "Ex 1.3 - Simple Contact With 2 Lifts and Multi-Lift" in {
    import `Ex 1.3 - Simple Contact with Multi-Lift-Kinds`._
    context.run(update, 2).tripleBatchMulti mustEqual List(
      (
        "UPDATE Contact AS p SET firstName = ps.firstName1, lastName = ps.lastName, age = ps.age FROM (VALUES (?, ?, ?, ?), (?, ?, ?, ?)) AS ps(firstName, firstName1, lastName, age) WHERE p.firstName = ps.firstName AND (p.firstName = ? OR p.firstName IN (?, ?))",
        List(
          List("Joe", "Joe", "BloggsU", 22, "Jan", "Jan", "RoggsU", 33, "Joe", "Dale", "Caboose"),
          List("James", "James", "JonesU", 44, "Dale", "Dale", "DomesU", 55, "Joe", "Dale", "Caboose")
        ), Unknown
      ), (
        "UPDATE Contact AS p SET firstName = ps.firstName1, lastName = ps.lastName, age = ps.age FROM (VALUES (?, ?, ?, ?)) AS ps(firstName, firstName1, lastName, age) WHERE p.firstName = ps.firstName AND (p.firstName = ? OR p.firstName IN (?, ?))",
        List(
          List("Caboose", "Caboose", "CastleU", 66, "Joe", "Dale", "Caboose")
        ), Unknown
      )
    )
  }

  "Ex 2 - Optional Embedded with Renames" in {
    import `Ex 2 - Optional Embedded with Renames`._
    context.run(update, 2).tripleBatchMulti.map(_._1.collapseSpace) mustEqual List(
      """UPDATE Contact AS p SET lastName = ps.name_last
        |FROM (VALUES (?, ?, ?, ?), (?, ?, ?, ?)) AS ps(name_first, name_first1, name_first2, name_last)
        |WHERE
        |  p.firstName IS NULL AND ps.name_first IS NULL OR
        |  p.firstName IS NOT NULL AND
        |  ps.name_first1 IS NOT NULL AND
        |  p.firstName = ps.name_first2
        |""".collapseSpace,
      """UPDATE Contact AS p SET lastName = ps.name_last
        |FROM (VALUES (?, ?, ?, ?)) AS ps(name_first, name_first1, name_first2, name_last)
        |WHERE
        |  p.firstName IS NULL AND ps.name_first IS NULL OR
        |  p.firstName IS NOT NULL AND
        |  ps.name_first1 IS NOT NULL AND
        |p.firstName = ps.name_first2
        |""".collapseSpace
    )
  }

  "Ex 4 - Returning" in {
    import `Ex 4 - Returning`._
    context.run(update, 2).tripleBatchMulti.map(_._1.collapseSpace) mustEqual List(
      """UPDATE Contact AS p SET firstName = ps.firstName1, lastName = ps.lastName, age = ps.age
        |FROM (VALUES (?, ?, ?, ?), (?, ?, ?, ?)) AS ps(firstName, firstName1, lastName, age)
        |WHERE p.firstName = ps.firstName
        |RETURNING ps.age
        |""".collapseSpace,
      """UPDATE Contact AS p SET firstName = ps.firstName1, lastName = ps.lastName, age = ps.age
        |FROM (VALUES (?, ?, ?, ?)) AS ps(firstName, firstName1, lastName, age)
        |WHERE p.firstName = ps.firstName
        |RETURNING ps.age
        |""".collapseSpace
    )
  }

  "Ex 5 - Append Data" in {
    import `Ex 5 - Append Data`._
    context.run(update, 2).tripleBatchMulti mustEqual List(
      (
        "UPDATE Contact AS p SET firstName = p.firstName || ps.firstName, lastName = p.lastName || ps.lastName FROM (VALUES (?, ?), (?, ?)) AS ps(firstName, lastName) WHERE p.firstName IN (?, ?, ?, ?, ?)",
        List(List("_A", "_B", "_AA", "_BB", "Joe", "Jan", "James", "Dale", "Caboose")),
        Unknown
      )
    )
  }
}
