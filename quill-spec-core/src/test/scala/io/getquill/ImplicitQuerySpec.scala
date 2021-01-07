package io.getquill

object iqContext extends MirrorContext(MirrorIdiom, Literal) with ImplicitQuery with TestEntities

object Test extends Function1[String, Test] {
  def apply(a: String) = new Test(a)
}

class Test(val a: String) extends Product {
  def canEqual(that: Any) = ???
  def productArity: Int = ???
  def productElement(n: Int) = ???
}

class ImplicitQuerySpec extends Spec {

  import iqContext._

  "allows querying a case class companion" in {
    val q = quote {
      TestEntity.filter(t => t.s == "s")
    }
    iqContext.run(q).string mustEqual
      """querySchema("TestEntity").filter(t => t.s == "s").map(t => (t.s, t.i, t.l, t.o, t.b))"""
  }

  "fails if querying a non-case-class companion" in {
    """
    val q = quote {
      Test.filter(_.a == "s")
    }
    """ mustNot compile
  }

  "only attempts to convert case class derived AbstractFunctionN to Query" - {

    "preserves inferred type of secondary join FunctionN argument" in {
      """
      val q = quote {
        for{
          (a,b)<- TestEntity.join(TestEntity2).on(_.i == _.i)
             c <- TestEntity3.leftJoin(_.i == a.i)
        } yield(a,b,c)
      }
      """ must compile
    }
  }
}
