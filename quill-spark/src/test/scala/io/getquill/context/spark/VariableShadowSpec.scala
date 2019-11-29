package io.getquill.context.spark

import io.getquill.Spec

case class American(firstName: String, lastName: String, addressId: Int)
case class Address1(id: Int, street: String, city: String)
case class HumanoidLivingSomewhere(called: String, alsoCalled: String, whereHeLives_id: Int)

class VariableShadowSpec extends Spec {

  import sqlContext.implicits._
  import testContext._

  val americansList = List(
    American("John", "James", 1),
    American("Joe", "Bloggs", 2),
    American("Roe", "Roggs", 3)
  )
  val addressesList = List(
    Address1(1, "1st Ave", "New York"),
    Address1(2, "2st Ave", "New Jersey")
  )

  val americans = quote { liftQuery(americansList.toDS()) }
  val addresses = quote { liftQuery(addressesList.toDS()) }

  val addressToSomeone = quote {
    (hum: Query[HumanoidLivingSomewhere]) =>
      for {
        t <- hum
        a <- addresses if (a.id == t.whereHeLives_id)
      } yield t
  }

  val americanClients = quote {
    addressToSomeone(americans.map(a => HumanoidLivingSomewhere(a.firstName, a.lastName, a.addressId))) //hellooo
  }

  "query should alias and function correctly" in {
    testContext.run(americanClients).collect().toList mustEqual List(
      HumanoidLivingSomewhere("John", "James", 1),
      HumanoidLivingSomewhere("Joe", "Bloggs", 2)
    )
  }

}
