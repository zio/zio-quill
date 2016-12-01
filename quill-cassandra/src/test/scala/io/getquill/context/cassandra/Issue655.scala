package io.getquill.context.cassandra

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric._

import io.getquill._

import scala.concurrent.ExecutionContext.Implicits.global

class Issue655 extends Spec {

  import testAsyncDB._

  type RefindedInterval = Interval.Closed[W.`0`.T, W.`200`.T]
  type RefinedIntervalType = Refined[Int, RefindedInterval]

  case class MyType(value: String)
  case class Person(id: Int, name: MyType, age: RefinedIntervalType)

  override def beforeAll = {
    await(testAsyncDB.run(query[Person].delete))
    ()
  }

  "Compile test" - {
    "empty" in {

      val age: RefinedIntervalType = 99
      val testPerson = Person(1, MyType("jj"), age)

      implicit val myTypeEncoder = MappedEncoding[MyType, String](_.value)
      implicit val myRefindedIntervalTypeEncoder = MappedEncoding[RefinedIntervalType, Int](_.get)

      await(testAsyncDB.run(query[Person].insert(lift(testPerson))).map(_ => ()))
      val deleteQ = quote(query[Person].filter(person => person.age == lift(age)).delete)
      await(testAsyncDB.run(deleteQ))

    }

  }
}