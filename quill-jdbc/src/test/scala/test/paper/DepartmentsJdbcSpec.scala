package test.paper

import test.Spec
import io.getquill.jdbc.JdbcSource
import io.getquill._
import test.testDB

case class Department(dpt: String)
case class Employee(emp: String, dpt: String, salary: Int)
case class Task(emp: String, tsk: String)

class DepartmentsJdbcSpec extends Spec {

  override def beforeAll =
    testDB.transaction {
      testDB.run(queryable[Department].delete)
      testDB.run(queryable[Employee].delete)
      testDB.run(queryable[Task].delete)

      testDB.run((dpt: String) => queryable[Department].insert(_.dpt -> dpt)) {
        List("Product", "Quality", "Research", "Sales")
      }
      testDB.run((dpt: String, emp: String) => queryable[Employee].insert(_.dpt -> dpt, _.emp -> emp)) {
        List(
          ("Product", "Alex"),
          ("Product", "Bert"),
          ("Research", "Cora"),
          ("Research", "Drew"),
          ("Research", "Edna"),
          ("Sales", "Fred")
        )
      }
      testDB.run((emp: String, tsk: String) => queryable[Task].insert(_.emp -> emp, _.tsk -> tsk)) {
        List(
          ("Alex", "build"),
          ("Bert", "build"),
          ("Cora", "abstract"),
          ("Cora", "build"),
          ("Cora", "design"),
          ("Drew", "abstract"),
          ("Drew", "design"),
          ("Edna", "abstract"),
          ("Edna", "call"),
          ("Edna", "design"),
          ("Fred", "call")
        )
      }
    }

  "Example 8 - nested naive" in {

    val expertiseNaive =
      quote {
        (u: String) =>
          for {
            d <- queryable[Department] if (
              (for {
                e <- queryable[Employee] if (
                  e.dpt == d.dpt && (
                    for {
                      t <- queryable[Task] if (e.emp == t.emp && t.tsk == u)
                    } yield {}).isEmpty)
              } yield {}).isEmpty)
          } yield d.dpt

      }

    testDB.run(expertiseNaive)("abstract") mustEqual List("Quality", "Research")
  }

  "Example 9 - nested db" in {

    val nestedOrg =
      quote {
        for {
          d <- queryable[Department]
        } yield {
          (d.dpt,
            for {
              e <- queryable[Employee] if (d.dpt == e.dpt)
            } yield {
              (e.emp,
                for {
                  t <- queryable[Task] if (e.emp == t.emp)
                } yield {
                  t.tsk
                }
              )
            }
          )
        }
      }

    val any =
      quote {
        new {
          def apply[T](xs: Queryable[T])(p: T => Boolean) =
            (for {
              x <- xs if (p(x))
            } yield {}).nonEmpty
        }
      }

    val all =
      quote {
        new {
          def apply[T](xs: Queryable[T])(p: T => Boolean) =
            !any(xs)(x => !p(x))
        }
      }

    def contains[T] =
      quote {
        new {
          def apply[T](xs: Queryable[T])(u: T) =
            any(xs)(x => x == u)
        }
      }

    val expertise =
      quote {
        (u: String) =>
          for {
            (dpt, employees) <- nestedOrg if (all(employees)(e => contains(e._2)(u)))
          } yield {
            dpt
          }
      }

    testDB.run(expertise)("abstract") mustEqual List("Quality", "Research")
  }
}