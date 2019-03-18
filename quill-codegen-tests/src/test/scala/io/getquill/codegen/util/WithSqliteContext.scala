package io.getquill.codegen.util

import java.io.Closeable

import io.getquill.codegen.integration.CodegenTestCases._
import io.getquill.codegen.util.ConfigPrefix.{ TestSqliteDB => TheDB }
import io.getquill.{ SnakeCase, SqliteDialect => TheDialect, SqliteJdbcContext => TheContext }
import javax.sql.DataSource

trait WithSqliteContext extends WithContextAux {
  import io.getquill.codegen.generated.sqlite._

  implicit def sqliteSimpleContextForTest1: Aux[TheDB, `1-simple-snake`, TheContext[SnakeCase]] =
    new WithContextBase[TheDB, `1-simple-snake`](TheDB, `1-simple-snake`) {
      override type QuillContext = TheContext[SnakeCase]
      override protected def makeContext(ds: DataSource with Closeable) = new QuillContext(SnakeCase, ds)
    }

  implicit def sqliteSimpleContextForTest2: Aux[TheDB, `2-simple-literal`, TheContext[SnakeCase]] =
    new WithContextBase[TheDB, `2-simple-literal`](TheDB, `2-simple-literal`) {
      override type QuillContext = TheContext[SnakeCase]
      override protected def makeContext(ds: DataSource with Closeable) = new QuillContext(SnakeCase, ds)
    }

  implicit def sqliteContextForTest1: Aux[TheDB, `1-comp-sanity`, TheContext[SnakeCase]] =
    new WithContextBase[TheDB, `1-comp-sanity`](TheDB, `1-comp-sanity`) {
      override type QuillContext = TheContext[SnakeCase]
      override protected def makeContext(ds: DataSource with Closeable) = new QuillContext(SnakeCase, ds)
    }

  implicit def sqliteContextForTest2: Aux[TheDB, `2-comp-stereo-single`, TheContext[SnakeCase] with `2-comp-stereo-single-lib`.public.PublicExtensions[TheDialect, SnakeCase]] =
    new WithContextBase[TheDB, `2-comp-stereo-single`](TheDB, `2-comp-stereo-single`) {
      override type QuillContext = TheContext[SnakeCase] with `2-comp-stereo-single-lib`.public.PublicExtensions[TheDialect, SnakeCase]
      override protected def makeContext(ds: DataSource with Closeable) = new TheContext[SnakeCase](SnakeCase, ds) with `2-comp-stereo-single-lib`.public.PublicExtensions[TheDialect, SnakeCase]
    }
}
