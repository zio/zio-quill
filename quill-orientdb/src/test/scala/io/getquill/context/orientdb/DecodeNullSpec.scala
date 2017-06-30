package io.getquill.context.orientdb

import io.getquill.Spec

class DecodeNullSpec extends Spec {

  "no default values when reading null" - {

    "sync" in {
      val ctx = orientdb.testSyncDB
      import ctx._
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

      ctx.run(writeEntities.delete)
      ctx.run(writeEntities.insert(lift(insertValue)))

      intercept[IllegalStateException] {
        ctx.run(query[DecodeNullTestEntity])
      }
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)
}