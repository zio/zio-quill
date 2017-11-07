package io.getquill.context.spark

import io.getquill.Spec

case class Department(dpt: String)
case class Employee(emp: String, dpt: String)
case class Task(emp: String, tsk: String)

class DepartmentsSparkSpec extends Spec {

  import testContext._
  import sqlContext.implicits._

  val departments = liftQuery {
    Seq(
      Department("Product"),
      Department("Quality"),
      Department("Research"),
      Department("Sales")
    ).toDS
  }

  val employees = liftQuery {
    Seq(
      Employee("Alex", "Product"),
      Employee("Bert", "Product"),
      Employee("Cora", "Research"),
      Employee("Drew", "Research"),
      Employee("Edna", "Research"),
      Employee("Fred", "Sales")
    ).toDS
  }

  val tasks = liftQuery {
    Seq(
      Task("Alex", "build"),
      Task("Bert", "build"),
      Task("Cora", "abstract"),
      Task("Cora", "build"),
      Task("Cora", "design"),
      Task("Drew", "abstract"),
      Task("Drew", "design"),
      Task("Edna", "abstract"),
      Task("Edna", "call"),
      Task("Edna", "design"),
      Task("Fred", "call")
    ).toDS
  }

  "Example 8 - nested naive" in {
    val q = quote {
      (u: String) =>
        for {
          d <- departments if (
            (for {
              e <- employees if (
                e.dpt == d.dpt && (
                  for {
                    t <- tasks if (e.emp == t.emp && t.tsk == u)
                  } yield {}
                ).isEmpty
              )
            } yield {}).isEmpty
          )
        } yield d.dpt
    }
    testContext.run(q("abstract")).collect().toList mustEqual
      List("Quality", "Research")
  }

  "Example 9 - nested db" in {
    val q = {
      val nestedOrg =
        quote {
          for {
            d <- departments
          } yield {
            (d.dpt,
              for {
                e <- employees if (d.dpt == e.dpt)
              } yield {
                (e.emp,
                  for {
                    t <- tasks if (e.emp == t.emp)
                  } yield {
                    t.tsk
                  })
              })
          }
        }

      def any[T] =
        quote { (xs: Query[T]) => (p: T => Boolean) =>
          (for {
            x <- xs if (p(x))
          } yield {}).nonEmpty
        }

      def all[T] =
        quote { (xs: Query[T]) => (p: T => Boolean) =>
          !any(xs)(x => !p(x))
        }

      def contains[T] =
        quote { (xs: Query[T]) => (u: T) =>
          any(xs)(x => x == u)
        }

      quote {
        (u: String) =>
          for {
            (dpt, employees) <- nestedOrg if (all(employees) { case (emp, tasks) => contains(tasks)(u) })
          } yield {
            dpt
          }
      }
    }
    testContext.run(q("abstract")).collect().toList mustEqual
      List("Quality", "Research")
  }
}
