package io.getquill

import io.getquill.context.Context
import io.getquill.quat.Quat

trait TestEntities {
  this: Context[_, _] =>

  case class TestEntity(s: String, i: Int, l: Long, o: Option[Int], b: Boolean)
  case class Emb(s: String, i: Int)
  case class TestEntityEmb(emb: Emb, l: Long, o: Option[Int])
  case class TestEntity2(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity3(s: String, i: Int, l: Long, o: Option[Int])
  case class TestEntity4(i: Long)
  case class TestEntity5(i: Long, s: String)
  case class EmbSingle(i: Long)
  case class TestEntity4Emb(emb: EmbSingle)
  case class TestEntityRegular(s: String, i: Long)

  private val QV  = Quat.Value
  private val QBV = Quat.BooleanValue

  val TestEntityQuat = Quat.Product("TestEntity", "s" -> QV, "i" -> QV, "l" -> QV, "o" -> QV, "b" -> QBV)
  val TestEntityEmbQuat =
    Quat.Product("TestEntityEmb", "emb" -> Quat.Product("Emb", "s" -> QV, "i" -> QV), "l" -> QV, "o" -> QV)
  val TestEntity2Quat    = Quat.Product("TestEntity2", "s" -> QV, "i" -> QV, "l" -> QV, "o" -> QV)
  val TestEntity3Quat    = Quat.Product("TestEntity3", "s" -> QV, "i" -> QV, "l" -> QV, "o" -> QV)
  val TestEntity4Quat    = Quat.Product("TestEntity4", "i" -> QV)
  val TestEntity5Quat    = Quat.Product("TestEntity5", "i" -> QV, "s" -> QV)
  val TestEntity4EmbQuat = Quat.Product("TestEntity4Emb", "emb" -> Quat.Product("EmbSingle", "i" -> QV))

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
