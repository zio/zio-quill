package io.getquill.context.sql
import io.getquill.Spec
import io.getquill.context.sql.testContext._
import io.getquill.Literal

class OrderBySpec extends Spec {

  implicit val naming = new Literal {}

  case class Country(id: Int, name: String)
  case class City(name: String, countryId: Int)

  "order by case class" in {
    val q = quote(
      query[City]
        .join(query[Country])
        .on { case (city, country) => city.countryId == country.id }
        .sortBy { case (city, country) => city }
    )
    testContext.run(q.dynamic).string mustEqual
      "SELECT x01.name, x01.countryId, x11.id, x11.name FROM City x01 INNER JOIN Country x11 ON x01.countryId = x11.id ORDER BY x01.name ASC NULLS FIRST, x01.countryId ASC NULLS FIRST"
  }

  "order by case class with rename" in {
    implicit val citySchema = schemaMeta[City]("theCity", _.countryId -> "theCityCode")
    implicit val countrySchema = schemaMeta[Country]("theCountry", _.id -> "theCountryCode")

    val q = quote(
      query[City]
        .join(query[Country])
        .on { case (city, country) => city.countryId == country.id }
        .sortBy { case (city, country) => city }
    )
    testContext.run(q.dynamic).string mustEqual
      "SELECT x03.name, x03.theCityCode, x12.theCountryCode, x12.name FROM theCity x03 INNER JOIN theCountry x12 ON x03.theCityCode = x12.theCountryCode ORDER BY x03.name ASC NULLS FIRST, x03.theCityCode ASC NULLS FIRST"
  }

}
