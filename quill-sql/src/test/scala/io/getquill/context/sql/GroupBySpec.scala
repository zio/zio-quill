package io.getquill.context.sql

import io.getquill.context.sql.testContext._
import io.getquill.Literal
import io.getquill.base.Spec
import io.getquill.context.sql.util.StringOps._

class GroupBySpec extends Spec {
  implicit val naming = new Literal {}

  import io.getquill.norm.{DisablePhase, OptionalPhase}
  import io.getquill.norm.ConfigList._

  "groupBy table expansion" - {
    case class Country(id: Int, name: String)
    case class City(name: String, countryId: Int)

    "basic" in {
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryId == country.id }
          .groupBy { case (city, country) => country }
          .map { case (country, cityInCountry) => (country.name, cityInCountry.map(cICn => cICn._1)) }
          .map { case (country, citiesInCountry) => (country, citiesInCountry.size) }
      )
      testContext.run(q).string mustEqual
        "SELECT x11.name AS _1, COUNT(x01.*) AS _2 FROM City x01 INNER JOIN Country x11 ON x01.countryId = x11.id GROUP BY x11.id, x11.name"
    }
    "with QuerySchema" in {
      implicit val citySchema    = schemaMeta[City]("theCity", _.name -> "theCityName")
      implicit val countrySchema = schemaMeta[Country]("theCountry", _.name -> "theCountryName")
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryId == country.id }
          .groupBy { case (city, country) => country }
          .map { case (country, cityInCountry) => (country.name, cityInCountry.map(cICn => cICn._1)) }
          .map { case (country, citiesInCountry) => (country, citiesInCountry.size) }
      )
      testContext.run(q).string mustEqual
        "SELECT x12.theCountryName AS _1, COUNT(x05.*) AS _2 FROM theCity x05 INNER JOIN theCountry x12 ON x05.countryId = x12.id GROUP BY x12.id, x12.theCountryName"
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
      testContext.run(q).string mustEqual
        "SELECT x010._2id AS id, x010._2name AS name, COUNT(x010.*) AS _2 FROM (SELECT x13.id AS _2id, x13.name AS _2name FROM City x09 INNER JOIN Country x13 ON x09.countryId = x13.id) AS x010 GROUP BY x010._2id, x010._2name"
    }
    "with QuerySchema nested" in {
      implicit val citySchema    = schemaMeta[City]("theCity", _.name -> "theCityName")
      implicit val countrySchema = schemaMeta[Country]("theCountry", _.name -> "theCountryName")
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryId == country.id }
          .nested
          .groupBy { case (city, country) => country }
          .map { case (country, citiesInCountry) => (country, citiesInCountry.size) }
      )
      testContext.run(q).string mustEqual
        "SELECT x013._2id AS id, x013._2theCountryName AS theCountryName, COUNT(x013.*) AS _2 FROM (SELECT x14.id AS _2id, x14.theCountryName AS _2theCountryName FROM theCity x012 INNER JOIN theCountry x14 ON x012.countryId = x14.id) AS x013 GROUP BY x013._2id, x013._2theCountryName"
    }

  }

  "Embedded entity expansion" - {
    case class Language(name: String, dialect: String)
    case class Country(countryCode: String, language: Language)
    case class City(countryCode: String, name: String)

    "simple" in {
      val q = quote(
        query[City]
          .join(query[Country])
          .on { case (city, country) => city.countryCode == country.countryCode }
          .groupBy { case (city, country) => country }
          .map { case (country, cityInCountry) =>
            ((country.countryCode, country.language), cityInCountry.map(cICn => cICn._1))
          }
          .map { case (country, cityCountries) => (country, cityCountries.size) }
      )
      testContext.run(q).string.collapseSpace mustEqual
        """
          |SELECT
          |  x15.countryCode AS _1,
          |  x15.name,
          |  x15.dialect,
          |  COUNT(x015.*) AS _2
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
          |  x020._2countryCode AS countryCode,
          |  x020._2languagename AS name,
          |  x020._2languagedialect AS dialect,
          |  COUNT(x020.*) AS _2
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
          .map { case (country, cityInCountry) =>
            ((country.countryCode, country.language), cityInCountry.map(cICn => cICn._1))
          }
          .map { case (country, cityCountries) => (country, cityCountries.size) }
      )
      testContext.run(q).string(true).collapseSpace mustEqual
        """|SELECT
            |  x17.theCountryCode AS _1,
            |  x17.TheLanguageName,
            |  x17.dialect,
            |  COUNT(x022.*) AS _2
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
          |  x027._2theCountryCode AS theCountryCode,
          |  x027._2languageTheLanguageName AS TheLanguageName,
          |  x027._2languagedialect AS dialect,
          |  COUNT(x027.*) AS _2
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
    testContext.run(q).string mustEqual
      "SELECT x19.countryCode, x19.language, COUNT(*) AS _2 FROM City x029 INNER JOIN CountryLanguage x19 ON x029.countryCode = x19.countryCode GROUP BY x19.countryCode, x19.language"
  }

  "map(transform).groupBy should work with" - {
    case class Person(id: Int, name: String, age: Int)

    "work with a groupBy(to-leaf).map.filter" in {
      testContext.run {
        query[Person]
          .groupBy(p => p.age)
          .map { case (_, ageList) => ageList.map(_.age).max.getOrNull }
          .filter(a => a > 1000)
      }.string mustEqual
        "SELECT p.* FROM (SELECT MAX(p.age) FROM Person p GROUP BY p.age) AS p WHERE p > 1000"
    }

    "work with a map(to-leaf).groupBy.map.filter - no ApplyMap" in {
      testContext.run {
        query[Person]
          .map(p => p.age)
          .groupBy(p => p)
          .map { case (_, ageList) => ageList.max.getOrNull }
          .filter(a => a > 1000)
      }.string mustEqual
        "SELECT p.* FROM (SELECT MAX(p.age) FROM Person p GROUP BY p.age) AS p WHERE p > 1000"
    }

    // Disable the apply-map phase to make sure these work in cases where this reduction is not possible (e.g. where they use infix etc...).
    // Infix has a special case already so want to not use that specifically.
    "work with a map(to-leaf).groupByMap.map.filter - no ApplyMap" in {
      implicit val d = new DisablePhase { override type Phase = OptionalPhase.ApplyMap :: HNil }
      testContext.run {
        query[Person]
          .map(p => p.age)
          .groupBy(p => p)
          .map { case (_, ageList) => ageList.max.getOrNull }
          .filter(a => a > 1000)
      }.string mustEqual
        "SELECT p.* FROM (SELECT MAX(p.age) FROM (SELECT p.age FROM Person p) AS p GROUP BY p.age) AS p WHERE p > 1000"
    }
  }
}
