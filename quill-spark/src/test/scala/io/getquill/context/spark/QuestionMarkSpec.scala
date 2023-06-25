package io.getquill.context.spark

import io.getquill.base.Spec
import org.scalatest.matchers.should.Matchers._

class QuestionMarkSpec extends Spec {
  val context = io.getquill.context.sql.testContext
  import testContext._
  import sqlContext.implicits._

  val peopleList = Seq(
    Contact("Alex", "Jones", 60, 2, "?"),
    Contact("Bert", "James", 55, 3, "bar")
  )

  val addressList = Seq(
    Address(1, "123 Fake Street", 11234, "?")
  )

  "simple variable usage must work" in {
    val q = quote {
      for {
        p <- liftQuery(peopleList.toDS()) if p.extraInfo == "?" && p.firstName == lift("Alex")
      } yield p
    }
    testContext.run(q).collect() should contain theSameElementsAs Seq(peopleList(0))
  }

  "simple variable usage must work in the middle of a string" in {
    val newContact      = Contact("Moe", "Rabbenu", 123, 2, "Something ? Something ? Else")
    val extraPeopleList = peopleList :+ newContact

    val q = quote {
      for {
        p <- liftQuery(extraPeopleList.toDS())
        if p.extraInfo == "Something ? Something ? Else" && p.firstName == lift("Moe")
      } yield p
    }
    testContext.run(q).collect() should contain theSameElementsAs Seq(newContact)
  }

  "lift usage" in {
    val q = quote {
      for {
        p <- liftQuery(peopleList.toDS()) if p.extraInfo == lift("?") && p.firstName == lift("Alex")
      } yield p
    }
    testContext.run(q).collect() should contain theSameElementsAs Seq(peopleList(0))
  }

  "infix usage must work" in {
    val q = quote {
      for {
        p <- liftQuery(peopleList.toDS()) if p.extraInfo == sql"'?'".as[String] && p.firstName == lift("Alex")
      } yield p
    }
    testContext.run(q).collect() should contain theSameElementsAs Seq(peopleList(0))
  }

  "simple join on question mark" in {
    val q = quote {
      for {
        p <- liftQuery(peopleList.toDS()) if p.extraInfo == "?" && p.firstName == lift("Alex")
        a <- liftQuery(addressList.toDS()) if p.extraInfo == a.otherExtraInfo
      } yield (p, a)
    }
    testContext.run(q).collect() should contain theSameElementsAs Seq((peopleList(0), addressList(0)))
  }
}
