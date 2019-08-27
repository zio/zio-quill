package io.getquill.context.sql.idiom

import io.getquill.Spec
import io.getquill.context.sql.testContextUpper
import io.getquill.context.sql.testContextUpper._

class NestedQueryNamingStrategySpec extends Spec {

  case class Person(id: Int, name: String)
  case class Address(ownerFk: Int, street: String)

  "inner aliases should use naming strategy" in {
    val q = quote {
      query[Person].map {
        p => (p, infix"foobar".as[Int])
      }.filter(_._1.id == 1)
    }
    testContextUpper.run(q).string mustEqual
      "SELECT p._1ID, p._1NAME, p._2 FROM (SELECT p.ID AS _1ID, p.NAME AS _1NAME, foobar AS _2 FROM PERSON p) AS p WHERE p._1ID = 1"
  }

  "inner aliases should use naming strategy with override" in {
    val qs = quote {
      querySchema[Person]("ThePerson", _.id -> "theId", _.name -> "theName") //helloooooo
    }

    val q = quote {
      qs.map {
        p => (p, infix"foobar".as[Int])
      }.filter(_._1.id == 1)
    }
    testContextUpper.run(q).string mustEqual
      "SELECT p._1theId, p._1theName, p._2 FROM (SELECT p.theId AS _1theId, p.theName AS _1theName, foobar AS _2 FROM ThePerson p) AS p WHERE p._1theId = 1"
  }

  "inner aliases should use naming strategy with override - two tables" in {
    val qs = quote {
      querySchema[Person]("ThePerson", _.id -> "theId", _.name -> "theName")
    }

    val joined = quote {
      qs.join(query[Person]).on((a, b) => a.name == b.name)
    }

    val q = quote {
      joined.map { (ab) =>
        val (a, b) = ab
        (a, b, infix"foobar".as[Int])
      }.filter(_._1.id == 1)
    }
    testContextUpper.run(q).string mustEqual
      "SELECT ab._1theId, ab._1theName, ab._2ID, ab._2NAME, ab._3 FROM (SELECT a.theId AS _1theId, a.theName AS _1theName, b.ID AS _2ID, b.NAME AS _2NAME, foobar AS _3 FROM ThePerson a INNER JOIN PERSON b ON a.theName = b.NAME) AS ab WHERE ab._1theId = 1"
  }

  "inner alias should nest properly in multiple levels" in {
    val q = quote {
      query[Person].map {
        p => (p, infix"foobar".as[Int])
      }.filter(_._1.id == 1).map {
        pp => (pp, infix"barbaz".as[Int])
      }.filter(_._1._1.id == 2)
    }

    testContextUpper.run(q).string mustEqual
      "SELECT p._1_1ID, p._1_1NAME, p._1_2, p._2 FROM (SELECT p._1ID AS _1_1ID, p._1NAME AS _1_1NAME, p._2 AS _1_2, barbaz AS _2 FROM (SELECT p.ID AS _1ID, p.NAME AS _1NAME, foobar AS _2 FROM PERSON p) AS p WHERE p._1ID = 1) AS p WHERE p._1_1ID = 2"
  }

  "inner alias should nest properly in multiple levels - with query schema" in {

    val qs = quote {
      querySchema[Person]("ThePerson", _.id -> "theId", _.name -> "theName")
    }

    val q = quote {
      qs.map {
        p => (p, infix"foobar".as[Int])
      }.filter(_._1.id == 1).map {
        pp => (pp, infix"barbaz".as[Int])
      }.filter(_._1._1.id == 2)
    }

    testContextUpper.run(q).string mustEqual
      "SELECT p._1_1theId, p._1_1theName, p._1_2, p._2 FROM (SELECT p._1theId AS _1_1theId, p._1theName AS _1_1theName, p._2 AS _1_2, barbaz AS _2 FROM (SELECT p.theId AS _1theId, p.theName AS _1theName, foobar AS _2 FROM ThePerson p) AS p WHERE p._1theId = 1) AS p WHERE p._1_1theId = 2"
  }
}
