package io.getquill

import java.io.Closeable
import javax.sql.DataSource

import org.scalamock.scalatest.MockFactory
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers

class JdbcContextSpec extends FlatSpec with MustMatchers with MockFactory {

  abstract class DataSourceWithCloseable extends DataSource with Closeable

  "JdbcContextSpec" should "call close when extends Closable" in {
    val dataSourceMock = mock[DataSourceWithCloseable]
    (dataSourceMock.close _).expects().once()

    val ctx = new JdbcContext[PostgresDialect, SnakeCase](dataSourceMock)
    ctx.close()
  }

}