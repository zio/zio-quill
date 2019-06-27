package io.getquill

class PluralizeSpec extends Spec {
  "pluralize" - {
    val s = new NamingStrategy with Pluralize

    "ending with something other than s" in {
      s.table("user") mustEqual "users"
    }
    "special inflection rules" in {
      s.table("activity") mustEqual "activities"
    }
  }
}
