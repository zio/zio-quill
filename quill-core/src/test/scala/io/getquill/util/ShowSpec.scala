package io.getquill.util

import io.getquill.Spec

class ShowSpec extends Spec {

  import Show._

  "given a Show implicit, provides an implicit class with the show method" in {
    implicit val show = Show[Int] {
      _.toString
    }
    1.show mustEqual "1"
  }

  "provides a factory method that receives a function" in {
    implicit val show = Show[Int](_.toString)
    1.show mustEqual "1"
  }
}
