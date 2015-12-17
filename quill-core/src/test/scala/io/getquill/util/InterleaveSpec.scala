package io.getquill.util

import io.getquill.Spec

class InterleaveSpec extends Spec {

  "same-size lists" in {
    val l1 = List(1, 3, 5)
    val l2 = List(2, 4, 6)
    Interleave(l1, l2) mustEqual List(1, 2, 3, 4, 5, 6)
  }

  "first list with more elements" in {
    val l1 = List(1, 3)
    val l2 = List(2)
    Interleave(l1, l2) mustEqual List(1, 2, 3)
  }

  "second list with more elements" in {
    val l1 = List(1)
    val l2 = List(2, 3)
    Interleave(l1, l2) mustEqual List(1, 2, 3)
  }
}
