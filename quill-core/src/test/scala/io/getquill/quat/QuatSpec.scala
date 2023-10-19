package io.getquill.quat

import io.getquill._
import io.getquill.base.Spec
import io.getquill.norm.RepropagateQuats
import io.getquill.quotation.QuatException

import scala.language.reflectiveCalls

class QuatSpec extends Spec {

  object testContext extends TestMirrorContextTemplate(MirrorIdiom, Literal) with TestEntities
  import testContext._

  "boolean and optional boolean" in {
    case class MyPerson(name: String, isHuman: Boolean, isRussian: Option[Boolean])
    val MyPersonQuat =
      Quat.Product("MyPerson", "name" -> Quat.Value, "isHuman" -> Quat.BooleanValue, "isRussian" -> Quat.BooleanValue)

    quote(query[MyPerson]).ast.quat mustEqual MyPersonQuat
    makeQuat[MyPerson] mustEqual MyPersonQuat
  }

  "should support standard case class" in {
    case class MyPerson(firstName: String, lastName: String, age: Int)
    val MyPersonQuat = Quat.LeafProduct("MyPerson", "firstName", "lastName", "age")

    quote(query[MyPerson]).ast.quat mustEqual MyPersonQuat
    makeQuat[MyPerson] mustEqual MyPersonQuat
  }

  "should support embedded" in {
    case class MyName(first: String, last: String)
    case class MyPerson(name: MyName, age: Int)
    val MyPersonQuat =
      Quat.Product("MyPerson", "name" -> Quat.LeafProduct("MyName", "first", "last"), "age" -> Quat.Value)

    quote(query[MyPerson]).ast.quat mustEqual MyPersonQuat
    makeQuat[MyPerson] mustEqual MyPersonQuat
  }

  "should refine quats from generic infixes" - {
    case class MyPerson(name: String, age: Int)
    val MyPersonQuat = Quat.Product("MyPerson", "name" -> Quat.Value, "age" -> Quat.Value)

    "propagating from transparent infixes in: extension methods" in {
      implicit class QueryOps[Q <: Query[_]](q: Q) {
        def appendFoo = quote(sql"$q APPEND FOO".transparent.pure.as[Q])
      }
      val q = quote(query[MyPerson].appendFoo)
      q.ast.quat mustEqual MyPersonQuat // I.e ReifyLiftings runs RepropagateQuats to take care of this
    }

    "not propagating from non-transparent infixes in: extension methods" in {
      implicit class QueryOps[Q <: Query[_]](q: Q) {
        def appendFoo = quote(sql"$q APPEND FOO".pure.as[Q])
      }
      val q = quote(query[MyPerson].appendFoo)
      q.ast.quat mustEqual Quat.Unknown
    }

    "not propagating from non-transparent infixes in: query-ops function" in {
      def appendFooFun[Q <: Query[_]] = quote((q: Q) => sql"$q APPEND FOO".pure.as[Q])
      val q                           = quote(appendFooFun(query[MyPerson]))
      q.ast.quat mustEqual Quat.Unknown
      // TODO Should be Unknown here
    }

    "propagating from transparent infixes in: query-ops function" in {
      def appendFooFun[Q <: Query[_]] = quote((q: Q) => sql"$q APPEND FOO".transparent.pure.as[Q])
      val q                           = quote(appendFooFun(query[MyPerson]))
      q.ast.quat mustEqual MyPersonQuat
    }

    "not propagating from transparent infixes in (where >1 param in the infix) in: query-ops function" in {
      def appendFooFun[Q <: Query[_]] = quote((q: Q, i: Int) => sql"$q APPEND $i FOO".transparent.pure.as[Q])
      val q                           = quote(appendFooFun(query[MyPerson], 123))
      q.ast.quat mustEqual Quat.Generic
    }

    "not propagating from transparent infixes where it is dynamic: query-ops function" in {
      // I.e. can't propagate from a dynamic query since don't know the inside of the quat variable
      def appendFooFun[Q <: Query[_]]: Quoted[Q => Q] = quote((q: Q) => sql"$q APPEND FOO".pure.as[Q])
      val q                                           = quote(appendFooFun(query[MyPerson]))
      q.ast.quat mustEqual Quat.Unknown
    }
  }

  "should support multi-level embedded" in {
    case class MyName(first: String, last: String)
    case class MyId(name: MyName, memberNum: Int)
    case class MyPerson(name: MyId, age: Int)
    val MyPersonQuat = Quat.Product(
      "MyPerson",
      "name" -> Quat.Product("MyId", "name" -> Quat.LeafProduct("MyName", "first", "last"), "memberNum" -> Quat.Value),
      "age"  -> Quat.Value
    )

    quote(query[MyPerson]).ast.quat mustEqual MyPersonQuat
  }

  "should support least upper types" - {
    val AnimalQuat = Quat.LeafProduct("AnimalQuat", "name")
    val CatQuat    = Quat.LeafProduct("Cat", "name", "color")

    "simple reduction" in {
      AnimalQuat.leastUpperType(CatQuat).get mustEqual AnimalQuat
      CatQuat.leastUpperType(AnimalQuat).get mustEqual AnimalQuat
    }

    "in query as" in {
      trait Animal { def name: String }
      case class Cat(name: String, color: Int) extends Animal

      def isSpot[A <: Animal] = quote { (animals: Query[A]) =>
        animals.filter(a => a.name == "Spot")
      }

      quote(isSpot[Cat](query[Cat])).ast.quat mustEqual CatQuat
    }
  }

  "lookup" - {
    val bar     = Quat.Product("bar", "baz" -> Quat.Value)
    val foo     = Quat.Product("foo", "v" -> Quat.Value, "bar" -> bar)
    val example = Quat.Product("example", "v" -> Quat.Value, "foo" -> foo)
    "path" in {
      example.lookup("foo", false) mustEqual foo
      example.lookup(List("foo", "bar"), false) mustEqual bar
      example.lookup(List("foo", "bar", "baz"), false) mustEqual Quat.Value
      example.lookup("blah", false) mustEqual Quat.Unknown
    }
  }

  "probit" in {
    val p: Quat = Quat.Product("p", "foo" -> Quat.Value)
    val v: Quat = Quat.Value
    p.probit mustEqual p
    val e = intercept[QuatException] {
      v.probit
    }
  }

  "rename" - {
    val prod = Quat.Product(
      "prod",
      "bv" -> Quat.BooleanValue,
      "be" -> Quat.BooleanExpression,
      "v"  -> Quat.Value,
      "p"  -> Quat.Product("innerProd", "vv" -> Quat.Value, "pp" -> Quat.Product("innerProd1", "ppp" -> Quat.Value))
    )
    val expect = Quat.Product(
      "expect",
      "bva" -> Quat.BooleanValue,
      "be"  -> Quat.BooleanExpression,
      "v"   -> Quat.Value,
      "pa"  -> Quat.Product("innerExpect", "vv" -> Quat.Value, "pp" -> Quat.Product("innerExpect1", "ppp" -> Quat.Value))
    )
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
    val example = Quat.Product(
      "outer",
      "bv" -> Quat.BooleanValue,
      "be" -> Quat.BooleanExpression,
      "v"  -> Quat.Value,
      "p"  -> Quat.Product("inner", "vv" -> Quat.Value)
    )
    "with boo" in {
      Quat.fromSerialized(serialize(example)) mustEqual example
    }
    // kryo tests are covered by standard JVM quill specs
  }

  "should support types in Query[T] position" - {
    "boolean value" in {
      def func = quote { (q: Query[Boolean]) =>
        q.filter(p => p == true)
      }
      func.ast.quat mustEqual Quat.BooleanValue
    }
    "boolean value - type" in {
      type Bool = Boolean
      def func = quote { (q: Query[Bool]) =>
        q.filter(p => p == true)
      }
      func.ast.quat mustEqual Quat.BooleanValue
    }
    "value" in {
      def func = quote { (q: Query[Int]) =>
        q.filter(p => p == 1)
      }
      func.ast.quat mustEqual Quat.Value
    }
    "structural with bool type" in {
      def func[T <: { def name: String; def isRussian: Boolean }] = quote { (q: Query[T]) =>
        q.filter(p => p.name == "Joe")
      }
      func.ast.quat mustEqual Quat.Generic
    }
    "structural with bool indirect" in {
      type Bool = Boolean
      def func[T <: { def name: String; def isRussian: Bool }] = quote { (q: Query[T]) =>
        q.filter(p => p.name == "Joe")
      }
      func.ast.quat mustEqual Quat.Generic
    }
    "case class" in {
      case class MyPerson(name: String, isRussian: Boolean)
      def func = quote { (q: Query[MyPerson]) =>
        q.filter(p => p.name == "Joe")
      }
      func.ast.quat mustEqual Quat.Product("MyPerson", "name" -> Quat.Value, "isRussian" -> Quat.BooleanValue)
    }
    "case class with boundary" in {
      case class MyPerson(name: String, isRussian: Boolean)
      def func[T <: MyPerson] = quote { (q: Query[T]) =>
        q.filter(p => p.name == "Joe")
      }
      func.ast.quat mustEqual Quat.Generic
    }
    "interface" in {
      trait LikePerson { def name: String; def isRussian: Boolean }
      def func = quote { (q: Query[LikePerson]) =>
        q.filter(p => p.name == "Joe")
      }
      func.ast.quat mustEqual Quat.Generic
    }
    "interface with boundary" in {
      trait LikePerson { def name: String; def isRussian: Boolean }
      def func[T <: LikePerson] = quote { (q: Query[T]) =>
        q.filter(p => p.name == "Joe")
      }
      func.ast.quat mustEqual Quat.Generic
    }
    "interface with boundary boolean indirect" in {
      type Bool = Boolean
      trait LikePerson { def name: String; def isRussian: Bool }
      def func[T <: LikePerson] = quote { (q: Query[T]) =>
        q.filter(p => p.name == "Joe")
      }
      func.ast.quat mustEqual Quat.Generic
    }
    "boundary with value" in {
      def func[T <: Int] = quote { (q: Query[T]) =>
        q
      }
      func.ast.quat mustEqual Quat.Value
    }
    "boundary with value - boolean" in {
      def func[T <: Boolean] = quote { (q: Query[T]) =>
        q
      }
      func.ast.quat mustEqual Quat.BooleanValue
    }
    "boundary with value and type - boolean" in {
      type Bool = Boolean
      def func[T <: Bool] = quote { (q: Query[T]) =>
        q
      }
      func.ast.quat mustEqual Quat.BooleanValue
    }
    "any" in {
      def func = quote { (q: Query[Any]) =>
        q
      }
      func.ast.quat mustEqual Quat.Generic
    }
  }

  "should support types" - {
    "boolean value" in {
      def func = quote { (q: Boolean) =>
        q
      }
      func.ast.quat mustEqual Quat.BooleanValue
    }
    "boolean value - type" in {
      type Bool = Boolean
      def func = quote { (q: Bool) =>
        q
      }
      func.ast.quat mustEqual Quat.BooleanValue
    }
    "value" in {
      def func = quote { (q: Int) =>
        q
      }
      func.ast.quat mustEqual Quat.Value
    }
    "structural with bool type" in {
      def func[T <: { def name: String; def isRussian: Boolean }] = quote { (q: T) =>
        q
      }
      // back here
      func.ast.quat mustEqual Quat
        .Product("T", "name" -> Quat.Value, "isRussian" -> Quat.BooleanValue)
        .withType(Quat.Product.Type.Abstract)
    }
    "structural with bool indirect" in {
      type Bool = Boolean
      def func[T <: { def name: String; def isRussian: Bool }] = quote { (q: T) =>
        q
      }
      func.ast.quat mustEqual Quat.Product("T", "name" -> Quat.Value, "isRussian" -> Quat.BooleanValue)
    }
    "case class" in {
      case class MyPerson(name: String, isRussian: Boolean)
      def func = quote { (q: MyPerson) =>
        q
      }
      func.ast.quat mustEqual Quat.Product("MyPerson", "name" -> Quat.Value, "isRussian" -> Quat.BooleanValue)
    }
    "case class with boundary" in {
      case class MyPerson(name: String, isRussian: Boolean)
      def func[T <: MyPerson] = quote { (q: T) =>
        q
      }
      func.ast.quat mustEqual Quat.Product("MyPerson", "name" -> Quat.Value, "isRussian" -> Quat.BooleanValue)
    }
    "interface" in {
      trait LikePerson { def name: String; def isRussian: Boolean }
      def func = quote { (q: LikePerson) =>
        q
      }
      func.ast.quat mustEqual Quat.Generic
    }
    "interface with boundary" in {
      trait LikePerson { def name: String; def isRussian: Boolean }
      def func[T <: LikePerson] = quote { (q: T) =>
        q
      }
      func.ast.quat mustEqual Quat.Product("LikePerson", "name" -> Quat.Value, "isRussian" -> Quat.BooleanValue)
    }
    "interface with boundary boolean indirect" in {
      type Bool = Boolean
      trait LikePerson { def name: String; def isRussian: Bool }
      def func[T <: LikePerson] = quote { (q: T) =>
        q
      }
      func.ast.quat mustEqual Quat.Product("LikePerson", "name" -> Quat.Value, "isRussian" -> Quat.BooleanValue)
    }
    "boundary with value" in {
      def func[T <: Int] = quote { (q: T) =>
        q
      }
      func.ast.quat mustEqual Quat.Value
    }
    "boundary with value - boolean" in {
      def func[T <: Boolean] = quote { (q: T) =>
        q
      }
      func.ast.quat mustEqual Quat.BooleanValue
    }
    "boundary with value and type - boolean" in {
      type Bool = Boolean
      def func[T <: Bool] = quote { (q: T) =>
        q
      }
      func.ast.quat mustEqual Quat.BooleanValue
    }
    "any" in {
      def func = quote { (q: Any) =>
        q
      }
      func.ast.quat mustEqual Quat.Generic
    }
  }
}
