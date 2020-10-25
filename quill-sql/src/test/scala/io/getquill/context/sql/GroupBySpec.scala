package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.sql.testContext._
import io.getquill.Literal
import io.getquill.context.sql.util.StringOps._

class GroupBySpec extends Spec {
  implicit val naming = new Literal {}

  "groupBy table expansion" - {
    case class Country(id: Int, name: String)
    case class City(name: String, countryId: Int)

    "basic" in {
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryId == country.id }
          .groupBy { case (city, country) => country }
          .map { case (country, citysInCountry) => (country.name, citysInCountry.map(cICn => cICn._1)) }
          .map { case (country, citiesInCountry) => (country, citiesInCountry.size) }
      )
      testContext.run(q.dynamic).string mustEqual
        "SELECT x11.name, COUNT(*) FROM City x01 INNER JOIN Country x11 ON x01.countryId = x11.id GROUP BY x11.id, x11.name"
    }
    "with QuerySchema" in {
      implicit val citySchema = schemaMeta[City]("theCity", _.name -> "theCityName")
      implicit val countrySchema = schemaMeta[Country]("theCountry", _.name -> "theCountryName")
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryId == country.id }
          .groupBy { case (city, country) => country }
          .map { case (country, citysInCountry) => (country.name, citysInCountry.map(cICn => cICn._1)) }
          .map { case (country, citiesInCountry) => (country, citiesInCountry.size) }
      )
      testContext.run(q.dynamic).string mustEqual
        "SELECT x12.theCountryName, COUNT(*) FROM theCity x05 INNER JOIN theCountry x12 ON x05.countryId = x12.id GROUP BY x12.id, x12.theCountryName"
    }
    "nested" in {
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryId == country.id }
          .nested
          .groupBy { case (city, country) => country }
          .map { case (country, citiesInCountry) => (country, citiesInCountry.size) }
      )
      testContext.run(q.dynamic).string mustEqual
        "SELECT x010._2id, x010._2name, COUNT(*) FROM (SELECT x13.id AS _2id, x13.name AS _2name FROM City x09 INNER JOIN Country x13 ON x09.countryId = x13.id) AS x010 GROUP BY x010._2id, x010._2name"
    }
    "with QuerySchema nested" in {
      implicit val citySchema = schemaMeta[City]("theCity", _.name -> "theCityName")
      implicit val countrySchema = schemaMeta[Country]("theCountry", _.name -> "theCountryName")
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryId == country.id }
          .nested
          .groupBy { case (city, country) => country }
          .map { case (country, citiesInCountry) => (country, citiesInCountry.size) }
      )
      testContext.run(q.dynamic).string mustEqual
        "SELECT x013._2id, x013._2theCountryName, COUNT(*) FROM (SELECT x14.id AS _2id, x14.theCountryName AS _2theCountryName FROM theCity x012 INNER JOIN theCountry x14 ON x012.countryId = x14.id) AS x013 GROUP BY x013._2id, x013._2theCountryName"
    }

  }

  "Embedded entity expansion" - {
    case class Language(name: String, dialect: String) extends Embedded
    case class Country(countryCode: String, language: Language)
    case class City(countryCode: String, name: String)

    "simple" in {
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryCode == country.countryCode }
          .groupBy { case (city, country) => country }
          .map { case (country, citysInCountry) => ((country.countryCode, country.language), citysInCountry.map(cICn => cICn._1)) }
          .map { case (country, cityCountries) => (country, cityCountries.size) }
      )
      testContext.run(q).string.collapseSpace mustEqual
        """
          |SELECT
          |  x15.countryCode,
          |  x15.name,
          |  x15.dialect,
          |  COUNT(*)
          |FROM
          |  City x015
          |  INNER JOIN Country x15 ON x015.countryCode = x15.countryCode
          |GROUP BY
          |  x15.countryCode,
          |  x15.name,
          |  x15.dialect
          |""".collapseSpace
    }
    "nested" in {
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryCode == country.countryCode }
          .nested
          .groupBy { case (city, country) => country }
          .map { case (country, cityCountries) => (country, cityCountries.size) }
      )
      testContext.run(q).string(true).collapseSpace mustEqual
        """
          |SELECT
          |  x020._2countryCode,
          |  x020._2languagename,
          |  x020._2languagedialect,
          |  COUNT(*)
          |FROM
          |  (
          |    SELECT
          |      x16.countryCode AS _2countryCode,
          |      x16.name AS _2languagename,
          |      x16.dialect AS _2languagedialect
          |    FROM
          |      City x019
          |      INNER JOIN Country x16 ON x019.countryCode = x16.countryCode
          |  ) AS x020
          |GROUP BY
          |  x020._2countryCode,
          |  x020._2languagename,
          |  x020._2languagedialect
          |""".collapseSpace
    }
    "with schema" in {
      implicit val countrySchema =
        schemaMeta[Country]("theCountry", _.countryCode -> "theCountryCode", _.language.name -> "TheLanguageName")

      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryCode == country.countryCode }
          .groupBy { case (city, country) => country }
          .map { case (country, citysInCountry) => ((country.countryCode, country.language), citysInCountry.map(cICn => cICn._1)) }
          .map { case (country, cityCountries) => (country, cityCountries.size) }
      )
      testContext.run(q).string(true).collapseSpace mustEqual
        """|SELECT
            |  x17.theCountryCode,
            |  x17.TheLanguageName,
            |  x17.dialect,
            |  COUNT(*)
            |FROM
            |  City x022
            |  INNER JOIN theCountry x17 ON x022.countryCode = x17.theCountryCode
            |GROUP BY
            |  x17.theCountryCode,
            |  x17.TheLanguageName,
            |  x17.dialect
            |""".collapseSpace
    }
    "with schema nested" in {
      implicit val languageSchema =
        schemaMeta[Country]("theCountry", _.countryCode -> "theCountryCode", _.language.name -> "TheLanguageName")

      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryCode == country.countryCode }
          .nested
          .groupBy { case (city, country) => country }
          .map { case (country, cityCountries) => (country, cityCountries.size) }
      )
      testContext.run(q).string(true).collapseSpace mustEqual
        """
          |SELECT
          |  x027._2theCountryCode,
          |  x027._2languageTheLanguageName,
          |  x027._2languagedialect,
          |  COUNT(*)
          |FROM
          |  (
          |    SELECT
          |      x18.theCountryCode AS _2theCountryCode,
          |      x18.TheLanguageName AS _2languageTheLanguageName,
          |      x18.dialect AS _2languagedialect
          |    FROM
          |      City x026
          |      INNER JOIN theCountry x18 ON x026.countryCode = x18.theCountryCode
          |  ) AS x027
          |GROUP BY
          |  x027._2theCountryCode,
          |  x027._2languageTheLanguageName,
          |  x027._2languagedialect
          |""".collapseSpace
    }
  }

  "after join" in {
    case class CountryLanguage(countryCode: String, language: String)
    case class City(id: Int, name: String, countryCode: String)

    val q = quote(
      query[City]
        .join(query[CountryLanguage])
        .on { case (city, cl) => city.countryCode == cl.countryCode }
        .groupBy { case (city, language) => language }
        .map { case (language, cityLanguages) => (language, cityLanguages.size) }
    )
    testContext.run(q.dynamic).string mustEqual
      "SELECT x19.countryCode, x19.language, COUNT(*) FROM City x029 INNER JOIN CountryLanguage x19 ON x029.countryCode = x19.countryCode GROUP BY x19.countryCode, x19.language"
  }

}
