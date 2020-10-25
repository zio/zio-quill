package io.getquill.context.spark

import io.getquill.Spec
import org.apache.spark.sql.Dataset
import scala.language.reflectiveCalls

case class Parent(name: String, childId: Int)
case class Child(name: String, id: Int)
case class GrandChild(name: String, parentId: Int)

class TypeMemberJoinSpec extends Spec {

  val context = io.getquill.context.sql.testContext

  import testContext._
  import sqlContext.implicits._

  sealed trait ChildJoiner {
    type SomeChild <: { def id: Int }
    type SomeGrandChild <: { def parentId: Int }
    def children: Dataset[SomeChild]
    def grandchildren: Dataset[SomeGrandChild]

    implicit class JoinFromParent[T <: { def childId: Int }](p: T) {
      def joinChild = quote {
        for {
          c <- liftQuery(children).join(c => c.id == p.childId)
        } yield c
      }
      def joinChildAndGrandChild = quote { //hellooooooo
        for {
          c <- liftQuery(children).join(c => c.id == p.childId)
          g <- liftQuery(grandchildren).join(g => g.parentId == c.id)
        } yield (c, g)
      }
    }
  }

  object Data {
    val parent = Parent("Joe", 1)
    val child = Child("Jack", 1)
    val grandChild = GrandChild("James", 1)
  }
  val parents = List(Data.parent).toDS
  val childrenBase = List(Data.child).toDS
  val grandChildrenBase = List(Data.grandChild).toDS

  class Extensions extends ChildJoiner {
    override type SomeChild = Child
    override type SomeGrandChild = GrandChild
    override val children: Dataset[Child] = childrenBase
    override val grandchildren: Dataset[GrandChild] = grandChildrenBase
  }

  "joins on type member objects" - {
    import io.getquill.ast.{ Ident => Id, _ }
    import io.getquill.ast.{ Ast, Infix }
    import io.getquill.quat.Quat
    object QStr {
      def unapply(q: Quat) = Some(q.toString)
    }
    object AnyInfix {
      def unapply(ast: Ast) =
        ast match {
          case Infix(_, _, _, _) => true
          case _                 => false
        }
    }

    val ext = new Extensions
    import ext._

    "should be possible on one object" in {
      val q = quote {
        for {
          p <- liftQuery(parents)
          c <- p.joinChild
        } yield (p, c)
      }

      q.ast match {
        case FlatMap(
          AnyInfix(),
          Id("p", QStr("CC(name:V,childId:V)")),
          Map(
            Map(
              FlatJoin(
                InnerJoin,
                AnyInfix(),
                Id("c", QStr("CCA(id:V)")), // This is important, since macro gets inferred from SomeChild, it only knows about the 'id' property
                (Property(Id("c", QStr("CCA(id:V)")), "id") +==+ Property(Id("p", QStr("CC(name:V,childId:V)")), "childId"))
                ),
              Id("c", QStr("CCA(id:V)")),
              Id("c", QStr("CCA(id:V)"))
              ),
            Id("c", QStr("CCA(name:V,id:V)")),
            Tuple(List(Id("p", QStr("CC(name:V,childId:V)")), Id("c", QStr("CCA(name:V,id:V)"))))
            )
          ) =>
        case _ =>
          fail(s"Tree did not match:\n${io.getquill.util.Messages.qprint(q.ast).plainText}")
      }
      testContext.run(q).collect().toList mustEqual List((Data.parent, Data.child))
    }

    "should be possible on one single-return object" - {
      "abstract" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield c
        }
        testContext.run(q).collect().toList mustEqual List(Data.child)
      }
      "concrete" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield p
        }
        testContext.run(q).collect().toList mustEqual List(Data.parent)
      }
      "abstract nested" in {
        val q = quote {
          (for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield c).nested
        }
        testContext.run(q).collect().toList mustEqual List(Data.child)
      }
      "abstract double nested" in {
        val q = quote {
          (for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield c).nested.nested
        }
        testContext.run(q).collect().toList mustEqual List(Data.child)
      }
      "concrete nested" in {
        val q = quote {
          (for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield p).nested
        }
        testContext.run(q).collect().toList mustEqual List(Data.parent)
      }
    }

    "should be possible on one single-return field" - {
      "abstract" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield c.name
        }
        testContext.run(q).collect().toList mustEqual List(Data.child.name)
      }
      "concrete" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield p.name
        }
        testContext.run(q).collect().toList mustEqual List(Data.parent.name)
      }
      "abstract nested" in {
        val q = quote {
          (for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield c.name).nested
        }
        testContext.run(q).collect().toList mustEqual List(Data.child.name)
      }
      "concrete nested" in {
        val q = quote {
          (for {
            p <- liftQuery(parents)
            c <- p.joinChild
          } yield p.name).nested
        }
        testContext.run(q).collect().toList mustEqual List(Data.parent.name)
      }
    }

    "should be possible on one multiple objects" in {
      val q = quote {
        for {
          p <- liftQuery(parents)
          (c, g) <- p.joinChildAndGrandChild
        } yield (p, c, g)
      }
      testContext.run(q).collect().toList mustEqual List((Data.parent, Data.child, Data.grandChild))
      testContext.run(q.nested).collect().toList mustEqual List((Data.parent, Data.child, Data.grandChild))
      testContext.run(q.nested.nested).collect().toList mustEqual List((Data.parent, Data.child, Data.grandChild))
    }

    "should be possible on one multiple objects - yielding a field" in {
      val q = quote {
        for {
          p <- liftQuery(parents)
          (c, g) <- p.joinChildAndGrandChild
        } yield (p, c.name)
      }

      q.ast match {
        case FlatMap(
          AnyInfix(),
          Id("p", QStr("CC(name:V,childId:V)")),
          Map(
            FlatMap(
              FlatJoin(
                InnerJoin,
                AnyInfix(),
                Id("c", QStr("CCA(id:V)")),
                (Property(Id("c", QStr("CCA(id:V)")), "id") +==+ Property(Id("p", QStr("CC(name:V,childId:V)")), "childId"))
                ),
              Id("c", QStr("CCA(id:V)")),
              Map(
                FlatJoin(
                  InnerJoin,
                  AnyInfix(),
                  Id("g", QStr("CCA(parentId:V)")),
                  (Property(Id("g", QStr("CCA(parentId:V)")), "parentId") +==+ Property(Id("c", QStr("CCA(id:V)")), "id"))
                  ),
                Id("g", QStr("CCA(parentId:V)")),
                Tuple(List(Id("c", QStr("CCA(id:V)")), Id("g", QStr("CCA(parentId:V)"))))
                )
              ),
            Id("x2", QStr("CC(_1:CCA(name:V,id:V),_2:CCA(name:V,parentId:V))")),
            Tuple(List(
              Id("p", QStr("CC(name:V,childId:V)")),
              // Note how here in the Quat of the inner ident, the _1 property representing 'child' has a 'name' and 'id' property
              // while Child of the _1 property in the 'c' tuple (in the inner Map) only has an 'id' property. This is because
              // when the quat of the _1 property above was synthesized, it was actually only the abstract property SomeChild
              // (i.e. while only has a 'id' property and no others)
              Property(Property(Id("x2", QStr("CC(_1:CCA(name:V,id:V),_2:CCA(name:V,parentId:V))")), "_1"), "name")
              ))
            )
          ) =>
        case _ =>
          fail(s"Tree did not match:\n${io.getquill.util.Messages.qprint(q.ast).plainText}")
      }
      testContext.run(q).collect().toList mustEqual List((Data.parent, Data.child.name))
    }

    "should be possible on one multiple objects" - {
      "single field - grandchild" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            (c, g) <- p.joinChildAndGrandChild
          } yield g.name
        }
        testContext.run(q).collect().toList mustEqual List((Data.grandChild.name))
      }

      "single field - child" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            (c, g) <- p.joinChildAndGrandChild
          } yield c.name
        }
        testContext.run(q).collect().toList mustEqual List((Data.child.name))
        testContext.run(q.nested).collect().toList mustEqual List((Data.child.name))
        testContext.run(q.nested.nested).collect().toList mustEqual List((Data.child.name))
      }

      "child and grandchild fields" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            (c, g) <- p.joinChildAndGrandChild
          } yield (c.name, g.name)
        }
        testContext.run(q).collect().toList mustEqual List((Data.child.name, Data.grandChild.name))
        testContext.run(q.nested).collect().toList mustEqual List((Data.child.name, Data.grandChild.name))
        testContext.run(q.nested.nested).collect().toList mustEqual List((Data.child.name, Data.grandChild.name))
      }

      "only parent" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            (c, g) <- p.joinChildAndGrandChild
          } yield p
        }
        testContext.run(q).collect().toList mustEqual List((Data.parent))
      }

      "mixed fields and values" in {
        val q = quote {
          for {
            p <- liftQuery(parents)
            (c, g) <- p.joinChildAndGrandChild
          } yield (p, c.name, g.name)
        }
        testContext.run(q).collect().toList mustEqual List((Data.parent, Data.child.name, Data.grandChild.name))
        testContext.run(q.nested).collect().toList mustEqual List((Data.parent, Data.child.name, Data.grandChild.name))
        testContext.run(q.nested.nested).collect().toList mustEqual List((Data.parent, Data.child.name, Data.grandChild.name))
      }
    }
  }

}
