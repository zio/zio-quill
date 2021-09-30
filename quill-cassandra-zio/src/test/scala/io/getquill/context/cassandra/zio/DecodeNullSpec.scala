package io.getquill.context.cassandra.zio

class DecodeNullSpec extends ZioCassandraSpec {

  "no default values when reading null" - {
    "stream" in {
      import testZioDB._
      val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

      val ret =
        for {
          _ <- testZioDB.run(writeEntities.delete)
          _ <- testZioDB.run(writeEntities.insert(lift(insertValue)))
          result <- testZioDB.run(query[DecodeNullTestEntity])
        } yield {
          result
        }

      result(ret.foldCause(
        cause => {
          cause.died must equal(true)
          cause.dieOption match {
            case Some(e: Exception) =>
              e.isInstanceOf[IllegalStateException] must equal(true)
            case _ =>
              fail("Expected Fatal Error to be here (and to be a IllegalStateException")
          }
        },
        success =>
          fail("Expected Exception IllegalStateException but operation succeeded")
      ))
      ()
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)
}

