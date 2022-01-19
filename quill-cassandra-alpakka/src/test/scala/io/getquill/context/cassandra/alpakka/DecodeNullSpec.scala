package io.getquill.context.cassandra.alpakka

class DecodeNullSpec extends CassandraAlpakkaSpec {

  "no default values when reading null" in {

    import testDB._
    val writeEntities = quote(querySchema[DecodeNullTestWriteEntity]("DecodeNullTestEntity"))

    val result =
      for {
        _ <- testDB.run(writeEntities.delete)
        _ <- testDB.run(writeEntities.insert(lift(insertValue)))
        result <- testDB.run(query[DecodeNullTestEntity])
      } yield {
        result
      }
    intercept[IllegalStateException] {
      await {
        result
      }
    }
  }

  case class DecodeNullTestEntity(id: Int, value: Int)

  case class DecodeNullTestWriteEntity(id: Int, value: Option[Int])

  val insertValue = DecodeNullTestWriteEntity(0, None)

}
