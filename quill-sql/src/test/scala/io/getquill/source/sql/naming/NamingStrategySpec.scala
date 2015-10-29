package io.getquill.source.sql.naming

import io.getquill.Spec

class NamingStrategySpec extends Spec {

  "escape" in {
    val s = new NamingStrategy with Escape

    s("value") mustEqual """"value""""
  }

  "upper case" in {
    val s = new NamingStrategy with UpperCase

    s("value") mustEqual "VALUE"
  }

  "lower case" in {
    val s = new NamingStrategy with LowerCase

    s("VALUE") mustEqual "value"
  }

  "snake case" - {

    val s = new NamingStrategy with SnakeCase

    "capitalized" in {
      s("SomeValue") mustEqual "some_value"
    }
    "non-capitalized" in {
      s("someValue") mustEqual "some_value"
    }
    "with number" in {
      s("someNumber123") mustEqual "some_number123"
    }
    "empty" in {
      s("") mustEqual ""
    }
    "sequence of upper case letters" in {
      s("ABCD") mustEqual "a_b_c_d"
    }
    "already snake case" in {
      s("some_value") mustEqual "some_value"
    }
  }

  "camel case" - {

    val s = new NamingStrategy with CamelCase

    "starting with _" in {
      s("_test") mustEqual "Test"
    }
    "multiple _" in {
      s("test__value") mustEqual "testValue"
    }
    "ending with _" in {
      s("test_") mustEqual "test"
    }
  }
}
