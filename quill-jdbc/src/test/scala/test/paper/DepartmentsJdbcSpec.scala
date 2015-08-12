package test.paper

import test.Spec
import io.getquill.jdbc.JdbcSource
import io.getquill._

case class Department(dpt: String)
case class Employee(emp: String, dpt: String, salary: Int)
case class Contact(dpt: String, contact: String, client: Int)
case class Task(emp: String, tsk: String)

class DepartmentsJdbcSpec extends Spec {

  object departmentsDB extends JdbcSource

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
                    } yield t).isEmpty)
              } yield e).isEmpty)
          } yield d

      }

    departmentsDB.run(expertiseNaive("a"))
    //    let expertiseNaive =
    //            <@ fun u ->
    //                query {
    //                    for d in db.Departments do
    //                        if not (query {
    //                                    for e in db.Employees do
    //                                        exists (e.Dpt = d.Dpt && not (query {
    //                                                                          for t in db.Tasks do
    //                                                                              exists (e.Emp = t.Emp && t.Tsk = u)
    //                                                                      }))
    //                                })
    //                        then yield d
    //                } @>
    //
    //        let ex8 = <@ query { yield! (%expertiseNaive) "abstract" } @>
  }

}