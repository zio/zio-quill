package io.getquill.quat

import io.getquill._
import io.getquill.quotation.QuatException

class QuatSpec extends Spec {

  object testContext extends TestMirrorContextTemplate(MirrorIdiom, Literal) with TestEntities
  import testContext._

  "boolean and optional boolean" in {
    case class MyPerson(name: String, isHuman: Boolean, isRussian: Option[Boolean])
    val MyPersonQuat = Quat.Product("name" -> Quat.Value, "isHuman" -> Quat.BooleanValue, "isRussian" -> Quat.BooleanValue)
    quote(query[MyPerson]).ast.quat mustEqual MyPersonQuat
  }

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

  "lookup" - {
    val bar = Quat.Product("baz" -> Quat.Value)
    val foo = Quat.Product("v" -> Quat.Value, "bar" -> bar)
    val example = Quat.Product("v" -> Quat.Value, "foo" -> foo)
    "path" in {
      example.lookup("foo") mustEqual foo
      example.lookup(List("foo", "bar")) mustEqual bar
      example.lookup(List("foo", "bar", "baz")) mustEqual Quat.Value
      example.lookup("blah") mustEqual Quat.Unknown
    }
  }

  "probit" in {
    val p: Quat = Quat.Product("foo" -> Quat.Value)
    val v: Quat = Quat.Value
    p.probit mustEqual p
    val e = intercept[QuatException] {
      v.probit
    }
  }

  "rename" - {
    val prod = Quat.Product("bv" -> Quat.BooleanValue, "be" -> Quat.BooleanExpression, "v" -> Quat.Value, "p" -> Quat.Product("vv" -> Quat.Value, "pp" -> Quat.Product("ppp" -> Quat.Value)))
    val expect = Quat.Product("bva" -> Quat.BooleanValue, "be" -> Quat.BooleanExpression, "v" -> Quat.Value, "pa" -> Quat.Product("vv" -> Quat.Value, "pp" -> Quat.Product("ppp" -> Quat.Value)))
    val value = Quat.Value
    "rename field" in {
      prod.withRenames(List("bv" -> "bva", "p" -> "pa")).applyRenames mustEqual expect
    }
    val e = intercept[QuatException] {
      value.withRenames(List("foo" -> "bar"))
    }
  }

  "should serialize" - {
    // Need to import implicits from BooQuatSerializer otherwise c_jl_UnsupportedOperationException happens in JS
    import BooQuatSerializer._
    val example = Quat.Product("bv" -> Quat.BooleanValue, "be" -> Quat.BooleanExpression, "v" -> Quat.Value, "p" -> Quat.Product("vv" -> Quat.Value))
    "with boo" in {
      Quat.fromSerializedJS(serialize(example)) mustEqual example
    }
    // kryo tests are covered by standard JVM quill specs
  }
}
