package io.getquill.sources.sql

import scala.language.reflectiveCalls

import io.getquill.Query
import io.getquill.Spec
import io.getquill.query
import io.getquill.quote
import io.getquill.unquote

trait DepartmentsSpec extends Spec {

  case class Department(dpt: String)
  case class Employee(emp: String, dpt: String, salary: Int)
  case class Task(emp: String, tsk: String)

  val departmentInsert =
    quote {
      (dpt: String) => query[Department].insert(_.dpt -> dpt)
    }

  val departmentEntries =
    List("Product", "Quality", "Research", "Sales")

  val employeeInsert =
    quote {
      (dpt: String, emp: String) => query[Employee].insert(_.dpt -> dpt, _.emp -> emp)
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
      (emp: String, tsk: String) => query[Task].insert(_.emp -> emp, _.tsk -> tsk)
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
          d <- query[Department] if (
            (for {
              e <- query[Employee] if (
                e.dpt == d.dpt && (
                  for {
                    t <- query[Task] if (e.emp == t.emp && t.tsk == u)
                  } yield {}).isEmpty)
            } yield {}).isEmpty)
        } yield d.dpt
    }

  val `Example 8 param` = "abstract"

  val `Example 8 expected result` = List("Quality", "Research")

  val any =
    quote {
      new {
        def apply[T](xs: Query[T])(p: T => Boolean) =
          (for {
            x <- xs if (p(x))
          } yield {}).nonEmpty
      }
    }

  val `Example 9 expertise` = {
    val nestedOrg =
      quote {
        for {
          d <- query[Department]
        } yield {
          (d.dpt,
            for {
              e <- query[Employee] if (d.dpt == e.dpt)
            } yield {
              (e.emp,
                for {
                  t <- query[Task] if (e.emp == t.emp)
                } yield {
                  t.tsk
                })
            })
        }
      }

    val all =
      quote {
        new {
          def apply[T](xs: Query[T])(p: T => Boolean) =
            !any(xs)(x => !p(x))
        }
      }

    def contains =
      quote {
        new {
          def apply[T](xs: Query[T])(u: T) =
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
