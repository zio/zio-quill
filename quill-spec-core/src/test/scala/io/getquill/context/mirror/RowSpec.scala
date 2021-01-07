package io.getquill.context.mirror

import io.getquill.Spec

class RowSpec extends Spec {

  "adds value" in {
    val r = Row(1, 2)
    r.add(3) mustEqual Row(1, 2, 3)
  }

  "gets value by index" in {
    val r = Row(1, 2)
    r[Int](0) mustEqual 1
  }

  "fails if the value doesn't match the expected type" in {
    val r = Row(1, 2)
    intercept[IllegalStateException] {
      r[String](0)
    }
    ()
  }
}
