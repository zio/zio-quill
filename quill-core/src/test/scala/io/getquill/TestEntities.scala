package io.getquill

import io.getquill.context.Context

trait TestEntities {
  this: Context[_, _] =>

  case class TestEntity(s: String, i: Int, l: Long, o: Option[Int])
  case class Emb(s: String, i: Int) extends Embedded
  case class TestEntityEmb(emb: Emb, l: Long, o: Option[Int])
  case class TestEntity2(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity3(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity4(i: Long)
  case class TestEntity5(i: Long, s: String)
  case class EmbSingle(i: Long) extends Embedded
  case class TestEntity4Emb(emb: EmbSingle)
  case class TestEntityRegular(s: String, i: Long)

  val qr1 = quote {
    query[TestEntity]
  }
  val qr1Emb = quote {
    querySchema[TestEntityEmb]("TestEntity")
  }
  val qr2 = quote {
    query[TestEntity2]
  }
  val qr3 = quote {
    query[TestEntity3]
  }
  val qr4 = quote {
    query[TestEntity4]
  }
  val qr5 = quote {
    query[TestEntity5]
  }
  val qr4Emb = quote {
    querySchema[TestEntity4Emb]("TestEntity4")
  }
  val qrRegular = quote {
    for {
      a <- query[TestEntity]
    } yield TestEntityRegular(a.s, a.l)
  }
}
