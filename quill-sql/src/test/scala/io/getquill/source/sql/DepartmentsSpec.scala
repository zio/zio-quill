package io.getquill.source.sql

import language.reflectiveCalls
import io.getquill._
import io.getquill.Queryable
import io.getquill.Spec
import io.getquill.unquote

trait DepartmentsSpec extends Spec {

  case class Department(dpt: String)
  case class Employee(emp: String, dpt: String, salary: Int)
  case class Task(emp: String, tsk: String)

  val departmentInsert =
    quote {
      (dpt: String) => queryable[Department].insert(_.dpt -> dpt)
    }

  val departmentEntries =
    List("Product", "Quality", "Research", "Sales")

  val employeeInsert =
    quote {
      (dpt: String, emp: String) => queryable[Employee].insert(_.dpt -> dpt, _.emp -> emp)
    }

  val employeeEntries =
    List(
      ("Product", "Alex"),
      ("Product", "Bert"),
      ("Research", "Cora"),
      ("Research", "Drew"),
      ("Research", "Edna"),
      ("Sales", "Fred"))

  val taskInsert =
    quote {
      (emp: String, tsk: String) => queryable[Task].insert(_.emp -> emp, _.tsk -> tsk)
    }

  val taskEntries =
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
      ("Fred", "call"))

  val `Example 8 expertise naive` =
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

  val `Example 8 param` = "abstract"

  val `Example 8 expected result` = List("Quality", "Research")

  val any =
    quote {
      new {
        def apply[T](xs: ComposableQueryable[T])(p: T => Boolean) =
          (for {
            x <- xs if (p(x))
          } yield {}).nonEmpty
      }
    }

  val `Example 9 expertise` = {
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
                })
            })
        }
      }

    val all =
      quote {
        new {
          def apply[T](xs: ComposableQueryable[T])(p: T => Boolean) =
            !any(xs)(x => !p(x))
        }
      }

    def contains[T] =
      quote {
        new {
          def apply[T](xs: ComposableQueryable[T])(u: T) =
            any(xs)(x => x == u)
        }
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

  val `Example 9 param` = "abstract"

  val `Example 9 expected result` = List("Quality", "Research")
}
