package io.getquill.sources

import io.getquill.Spec

class BindedStatementBuilderSpec extends Spec {

  val rawEnc =
    new Encoder[List[Int], Int] {
      override def apply(idx: Int, v: Int, p: List[Int]) =
        p :+ v
    }

  val enc =
    new Encoder[BindedStatementBuilder[List[Int]], Int] {
      override def apply(idx: Int, v: Int, p: BindedStatementBuilder[List[Int]]) =
        p.single(idx, v, rawEnc)
    }

  class Subject extends BindedStatementBuilder[List[Int]]

  "no-op for non-binded queries" in {
    val subject = new Subject
    val query = "SELECT * FROM Test"

    val (expanded, bind) = subject.build(query)

    expanded mustEqual query
    bind(List()) mustEqual List()
  }

  "single binding" in {
    val subject = new Subject
    val query = "SELECT * FROM Test WHERE id = ?"

    subject.single(0, 1, rawEnc)
    val (expanded, bind) = subject.build(query)

    expanded mustEqual query

    bind(List()) mustEqual List(1)
  }

  "set binding" in {
    val subject = new Subject
    val query = "SELECT * FROM Test WHERE id IN (?)"

    subject.coll(0, Set(1, 2), enc)

    val (expanded, bind) = subject.build(query)

    expanded mustEqual "SELECT * FROM Test WHERE id IN (?, ?)"
    bind(List()) mustEqual List(1, 2)
  }

  "empty set binding" in {
    val subject = new Subject
    val query = "SELECT * FROM Test WHERE id IN (?)"

    subject.coll(0, Set.empty[Int], enc)

    val (expanded, bind) = subject.build(query)

    expanded mustEqual "SELECT * FROM Test WHERE FALSE"
    bind(List()) mustEqual List()
  }

  "mixed" in {
    val subject = new Subject
    val query = "SELECT age - ? FROM Test WHERE id IN (?) AND age > ?"

    subject.single(0, 1, rawEnc)
    subject.coll(1, Set(2, 3), enc)
    subject.single(2, 4, rawEnc)

    val (expanded, bind) = subject.build(query)

    expanded mustEqual "SELECT age - ? FROM Test WHERE id IN (?, ?) AND age > ?"
    bind(List()) mustEqual List(1, 2, 3, 4)
  }

  "ignores quote" in {
    val subject = new Subject
    val query = "SELECT '?' FROM Test WHERE id = ?"

    subject.single(0, 1, rawEnc)
    val (expanded, bind) = subject.build(query)

    expanded mustEqual query

    bind(List()) mustEqual List(1)
  }

  "ignores quote within quote" in {
    val subject = new Subject
    val query = "SELECT '\\'?\\' \\'' FROM Test WHERE id = ?"

    subject.single(0, 1, rawEnc)
    val (expanded, bind) = subject.build(query)

    expanded mustEqual query

    bind(List()) mustEqual List(1)
  }

  "invalid" - {
    "more ?" in {
      val subject = new Subject
      val query = "SELECT age - ? FROM Test WHERE id IN (?)"

      subject.coll(1, Set(1, 2), enc)

      intercept[IllegalStateException] {
        subject.build(query)
      }
      ()
    }
    "less ?" in {
      val subject = new Subject
      val query = "SELECT age FROM Test WHERE id IN (?)"

      subject.single(0, 1, rawEnc)
      subject.coll(1, Set(1, 2), enc)

      intercept[IllegalStateException] {
        subject.build(query)
      }
      ()
    }
  }
}
