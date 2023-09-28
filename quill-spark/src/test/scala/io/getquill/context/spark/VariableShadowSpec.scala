package io.getquill.context.spark

import io.getquill.Query
import io.getquill.base.Spec
import io.getquill.Quoted

final case class American(firstName: String, lastName: String, addressId: Int)
final case class Address1(id: Int, street: String, city: String)
final case class HumanoidLivingSomewhere(called: String, alsoCalled: String, whereHeLives_id: Int)

class VariableShadowSpec extends Spec {

  import sqlContext.implicits._
  import testContext._

  val americansList: List[American] = List(
    American("John", "James", 1),
    American("Joe", "Bloggs", 2),
    American("Roe", "Roggs", 3)
  )
  val addressesList: List[Address1] = List(
    Address1(1, "1st Ave", "New York"),
    Address1(2, "2st Ave", "New Jersey")
  )

  val americans: Quoted[Query[American]] = quote(liftQuery(americansList.toDS()))
  val addresses: Quoted[Query[Address1]] = quote(liftQuery(addressesList.toDS()))

  val addressToSomeone: Quoted[Query[HumanoidLivingSomewhere] => Query[HumanoidLivingSomewhere]] = quote {
    (hum: Query[HumanoidLivingSomewhere]) =>
      for {
        t <- hum
        a <- addresses if (a.id == t.whereHeLives_id)
      } yield t
  }

  val americanClients: Quoted[Query[HumanoidLivingSomewhere]] = quote {
    addressToSomeone(americans.map(a => HumanoidLivingSomewhere(a.firstName, a.lastName, a.addressId))) // hellooo
  }

  "query should alias and function correctly" in {
    testContext.run(americanClients).collect().toList mustEqual List(
      HumanoidLivingSomewhere("John", "James", 1),
      HumanoidLivingSomewhere("Joe", "Bloggs", 2)
    )
  }

}
