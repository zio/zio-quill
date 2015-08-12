package test.paper

import test.Spec
import io.getquill.jdbc.JdbcSource
import io.getquill._

case class Department(dpt: String)
case class Employee(emp: String, dpt: String, salary: Int)
case class Contact(dpt: String, contact: String, client: Int)
case class Task(emp: String, tsk: String)

class DepartmentsJdbcSpec extends Spec {

  "Example 8 - nested naive" ignore {
    object departmentsDB extends JdbcSource

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
          } yield d

      }

    departmentsDB.run(expertiseNaive("a"))
  }
}