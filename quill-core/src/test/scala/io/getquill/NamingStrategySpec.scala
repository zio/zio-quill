package io.getquill

class NamingStrategySpec extends Spec {

  "uses the default impl" in {
    val s = new NamingStrategy with LowerCase

    s.table("VALUE") mustEqual "value"
    s.column("VALUE") mustEqual "value"
  }

  "can override table strategy" in {
    val s = new NamingStrategy with LowerCase {
      override def table(s: String) = s
    }

    s.table("VALUE") mustEqual "VALUE"
    s.column("VALUE") mustEqual "value"
  }

  "can override column strategy" in {
    val s = new NamingStrategy with LowerCase {
      override def column(s: String) = s
    }

    s.column("VALUE") mustEqual "VALUE"
    s.table("VALUE") mustEqual "value"
  }

  "escape" in {
    val s = new NamingStrategy with Escape

    s.default("value") mustEqual """"value""""
  }

  "upper case" in {
    val s = new NamingStrategy with UpperCase

    s.default("value") mustEqual "VALUE"
  }

  "lower case" in {
    val s = new NamingStrategy with LowerCase

    s.default("VALUE") mustEqual "value"
  }

  "snake case" - {

    val s = new NamingStrategy with SnakeCase

    "capitalized" in {
      s.default("SomeValue") mustEqual "some_value"
    }
    "non-capitalized" in {
      s.default("someValue") mustEqual "some_value"
    }
    "with number" in {
      s.default("someNumber123") mustEqual "some_number123"
    }
    "empty" in {
      s.default("") mustEqual ""
    }
    "sequence of upper case letters" in {
      s.default("ABCD") mustEqual "a_b_c_d"
    }
    "already snake case" in {
      s.default("some_value") mustEqual "some_value"
    }
  }

  "camel case" - {

    val s = new NamingStrategy with CamelCase

    "starting with _" in {
      s.default("_test") mustEqual "Test"
    }
    "multiple _" in {
      s.default("test__value") mustEqual "testValue"
    }
    "ending with _" in {
      s.default("test_") mustEqual "test"
    }
  }

  "pluralized table names" - {
    val s = new NamingStrategy with PluralizedTableNames

    "ending with s" in {
      s.table("kittens") mustEqual "kittens"
    }

    "ending with something other than s" in {
      s.table("kitten") mustEqual "kittens"
    }
  }

  "postgres quote" - {
    val s = new NamingStrategy with PostgresEscape

    "preserve default naming strategy" in {
      s.column("test") mustEqual """"test""""
    }
    "unquote column name starting with $" in {
      s.column("$1") mustEqual "$1"
    }
  }

  "mysql quote" - {
    val s = new NamingStrategy with MysqlEscape

    "quote table name with ``" in {
      s.table("order") mustEqual "`order`"
    }

    "quote column name with ``" in {
      s.column("count") mustEqual "`count`"
    }

    "preserve default naming strategy" in {
      s.default("test") mustEqual ("test")
    }
  }

  "composite" - {
    val s = NamingStrategy(SnakeCase, MysqlEscape)

    "table name" in {
      s.table("someValue") mustEqual "`some_value`"
    }

    "column name" in {
      s.column("someValue") mustEqual "`some_value`"
    }

    "default" in {
      s.default("someValue") mustEqual "some_value"
    }
  }
}
