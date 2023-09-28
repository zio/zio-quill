package io.getquill

import io.getquill.context.Context
import io.getquill.quat.Quat

trait TestEntities {
  this: Context[_, _] =>

  case class TestEntity(s: String, i: Int, l: Long, o: Option[Int], b: Boolean)

  lazy val testEntitySchemaMeta: SchemaMeta[TestEntity] = schemaMeta[TestEntity](
    "TestEntity",
    _.s -> "s"
  )

  lazy val testEntityQuerySchema: Quoted[EntityQuery[TestEntity]] = quote(
    querySchema[TestEntity](
      "TestEntity",
      _.s -> "s",
      _.i -> "i",
      _.l -> "l",
      _.o -> "o",
      _.b -> "b"
    )
  )
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

  val TestEntityQuat: Quat.Product = Quat.Product("TestEntity", "s" -> QV, "i" -> QV, "l" -> QV, "o" -> QV, "b" -> QBV)
  val TestEntityEmbQuat: Quat.Product =
    Quat.Product("TestEntityEmb", "emb" -> Quat.Product("Emb", "s" -> QV, "i" -> QV), "l" -> QV, "o" -> QV)
  val TestEntity2Quat: Quat.Product    = Quat.Product("TestEntity2", "s" -> QV, "i" -> QV, "l" -> QV, "o" -> QV)
  val TestEntity3Quat: Quat.Product    = Quat.Product("TestEntity3", "s" -> QV, "i" -> QV, "l" -> QV, "o" -> QV)
  val TestEntity4Quat: Quat.Product    = Quat.Product("TestEntity4", "i" -> QV)
  val TestEntity5Quat: Quat.Product    = Quat.Product("TestEntity5", "i" -> QV, "s" -> QV)
  val TestEntity4EmbQuat: Quat.Product = Quat.Product("TestEntity4Emb", "emb" -> Quat.Product("EmbSingle", "i" -> QV))

  val qr1: Quoted[EntityQuery[TestEntity]] = quote {
    query[TestEntity]
  }
  val qr1Emb: Quoted[EntityQuery[TestEntityEmb]] = quote {
    querySchema[TestEntityEmb]("TestEntity")
  }
  val qr2: Quoted[EntityQuery[TestEntity2]] = quote {
    query[TestEntity2]
  }
  val qr3: Quoted[EntityQuery[TestEntity3]] = quote {
    query[TestEntity3]
  }
  val qr4: Quoted[EntityQuery[TestEntity4]] = quote {
    query[TestEntity4]
  }
  val qr5: Quoted[EntityQuery[TestEntity5]] = quote {
    query[TestEntity5]
  }
  val qr4Emb: Quoted[EntityQuery[TestEntity4Emb]] = quote {
    querySchema[TestEntity4Emb]("TestEntity4")
  }
  val qrRegular: Quoted[EntityQuery[TestEntityRegular]] = quote {
    for {
      a <- query[TestEntity]
    } yield TestEntityRegular(a.s, a.l)
  }
}
