package io.getquill.context.orientdb

import java.util.Date

import io.getquill.Spec

class SetsEncodingSpec extends Spec {

  case class SetsEntity(
    id:         Int,
    texts:      Set[String],
    bools:      Set[Boolean],
    ints:       Set[Int],
    longs:      Set[Long],
    doubles:    Set[Double],
    timestamps: Set[Date]
  )

  val e = SetsEntity(1, Set("c"), Set(true), Set(1), Set(2),
    Set(5.5d), Set(new Date()))

  private def verify(expected: SetsEntity, actual: SetsEntity): Boolean = {
    expected.id mustEqual actual.id
    expected.texts mustEqual actual.texts
    expected.bools mustEqual actual.bools
    expected.ints mustEqual actual.ints
    expected.longs mustEqual actual.longs
    expected.doubles mustEqual actual.doubles
    expected.timestamps.isInstanceOf[Date]
  }

  "mirror" in {
    val ctx = orientdb.mirrorContext
    import ctx._
    val q = quote(query[SetsEntity])
    ctx.run(q.insert(lift(e)))
    ctx.run(q)
  }

  "Set encoders/decoders" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    val q = quote(query[SetsEntity])
    ctx.run(q.delete)
    ctx.run(q.insert(lift(e)))
    verify(e, ctx.run(q.filter(_.id == 1)).head)
  }

  "Empty Lists and optional fields" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    case class Entity(id: Int, texts: Option[List[String]], bools: Option[List[String]])
    val e = Entity(1, Some(List("1", "2")), None)
    val q = quote(querySchema[Entity]("ListEntity"))

    ctx.run(q.delete)
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1)).head mustBe e
  }

  "Blob (Array[Byte]) support" ignore {
    val ctx = orientdb.testSyncDB
    import ctx._
    case class BlobsEntity(id: Int, blobs: List[Array[Byte]])
    val e = BlobsEntity(1, List(Array(1.toByte, 2.toByte), Array(2.toByte)))
    val q = quote(querySchema[BlobsEntity]("BlobsEntity"))

    ctx.run(q.delete)
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(_.id == 1))
      .head.blobs.map(_.toList) mustBe e.blobs.map(_.toList)
  }

  "Set in where clause" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    case class ListFrozen(id: List[Int])
    val e = ListFrozen(List(1, 2))
    val q = quote(query[ListFrozen])
    ctx.run(q.delete)
    ctx.run(q.insert(lift(e)))
    ctx.run(q.filter(p => liftQuery(Set(1)).contains(p.id))) mustBe List(e)
    ctx.run(q.filter(_.id == lift(List(1)))) mustBe Nil
  }
}