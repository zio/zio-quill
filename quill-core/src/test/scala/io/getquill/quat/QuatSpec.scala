package io.getquill.quat

import io.getquill._

class QuatSpec extends Spec {

  object testContext extends TestMirrorContextTemplate(MirrorIdiom, Literal) with TestEntities
  import testContext._

  "should support standard case class" in {
    case class MyPerson(firstName: String, lastName: String, age: Int)
    val MyPersonQuat = Quat.LeafProduct("firstName", "lastName", "age")

    quote(query[MyPerson]).ast.quat mustEqual MyPersonQuat
  }

  "should support embedded" in {
    case class MyName(first: String, last: String) extends Embedded
    case class MyPerson(name: MyName, age: Int) extends Embedded
    val MyPersonQuat = Quat.Product("name" -> Quat.LeafProduct("first", "last"), "age" -> Quat.Value)

    quote(query[MyPerson]).ast.quat mustEqual MyPersonQuat
  }

  "should support multi-level embedded" in {
    case class MyName(first: String, last: String) extends Embedded
    case class MyId(name: MyName, memberNum: Int) extends Embedded
    case class MyPerson(name: MyId, age: Int)
    val MyPersonQuat = Quat.Product("name" -> Quat.Product("name" -> Quat.LeafProduct("first", "last"), "memberNum" -> Quat.Value), "age" -> Quat.Value)

    quote(query[MyPerson]).ast.quat mustEqual MyPersonQuat
  }

  "should support least upper types" - {
    val AnimalQuat = Quat.LeafProduct("name")
    val CatQuat = Quat.LeafProduct("name", "color")

    "simple reduction" in {
      AnimalQuat.leastUpperType(CatQuat).get mustEqual AnimalQuat
      CatQuat.leastUpperType(AnimalQuat).get mustEqual AnimalQuat
    }

    "in query as" in {
      trait Animal { def name: String }
      case class Cat(name: String, color: Int) extends Animal

      def isSpot[A <: Animal] = quote {
        (animals: Query[A]) => animals.filter(a => a.name == "Spot")
      }

      quote(isSpot[Cat](query[Cat])).ast.quat mustEqual CatQuat
    }

  }
}
