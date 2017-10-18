package io.getquill.context.orientdb

import java.util.Date

import io.getquill.Spec

class ListsEncodingSpec extends Spec {

  case class ListsEntity(
    id:         Int,
    texts:      List[String],
    bools:      List[Boolean],
    ints:       List[Int],
    longs:      List[Long],
    floats:     List[Float],
    doubles:    List[Double],
    timestamps: List[Date]
  )
  val e = ListsEntity(1, List("c"), List(true), List(1, 2), List(2, 3), List(1.2f, 3.2f),
    List(5.1d), List(new Date()))

  private def verify(expected: ListsEntity, actual: ListsEntity): Boolean = {
    expected.id mustEqual actual.id
    expected.texts mustEqual actual.texts
    expected.bools mustEqual actual.bools
    expected.ints mustEqual actual.ints
    expected.longs mustEqual actual.longs
    expected.doubles mustEqual actual.doubles
    actual.timestamps.head.isInstanceOf[Date]
    true
  }

  "mirror" in {
    val ctx = orientdb.mirrorContext
    import ctx._
    val q = quote(query[ListsEntity])
    ctx.run(q.insert(lift(e)))
    ctx.run(q)
  }

  "List encoders/decoders for OrientDB Types" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    val q = quote(query[ListsEntity])
    ctx.run(q.delete)
    ctx.run(q.insert(lift(e)))
    verify(e, ctx.run(q.filter(_.id == 1)).head)
  }

  "Empty Lists and optional fields" in {
    val ctx = orientdb.testSyncDB
    import ctx._
    case class Entity(id: Int, texts: Option[List[String]], bools: Option[List[Boolean]])
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

  "List in where clause" in {
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