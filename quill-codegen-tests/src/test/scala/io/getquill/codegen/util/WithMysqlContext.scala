package io.getquill.codegen.util

import java.io.Closeable

import io.getquill.{ MySQLDialect => TheDialect, MysqlJdbcContext => TheContext, Literal, SnakeCase }
import io.getquill.codegen.integration.CodegenTestCases._
import io.getquill.codegen.util.ConfigPrefix.{ TestMysqlDB => TheDB }
import javax.sql.DataSource

trait WithMysqlContext extends WithContextAux {
  import io.getquill.codegen.generated.mysql._

  implicit def mysqlSimpleContextForTest1: Aux[TheDB, `1-simple-snake`, TheContext[SnakeCase]] =
    new WithContextBase[TheDB, `1-simple-snake`](TheDB, `1-simple-snake`) {
      override type QuillContext = TheContext[SnakeCase]
      override protected def makeContext(ds: DataSource with Closeable) = new QuillContext(SnakeCase, ds)
    }

  implicit def mysqlSimpleContextForTest2: Aux[TheDB, `2-simple-literal`, TheContext[SnakeCase]] =
    new WithContextBase[TheDB, `2-simple-literal`](TheDB, `2-simple-literal`) {
      override type QuillContext = TheContext[SnakeCase]
      override protected def makeContext(ds: DataSource with Closeable) = new QuillContext(SnakeCase, ds)
    }

  implicit def mysqlContextForTest1: Aux[TheDB, `1-comp-sanity`, TheContext[SnakeCase]] =
    new WithContextBase[TheDB, `1-comp-sanity`](TheDB, `1-comp-sanity`) {
      override type QuillContext = TheContext[SnakeCase]
      override protected def makeContext(ds: DataSource with Closeable) = new QuillContext(SnakeCase, ds)
    }

  implicit def mysqlContextForTest2: Aux[TheDB, `2-comp-stereo-single`, TheContext[SnakeCase] with `2-comp-stereo-single-lib`.public.PublicExtensions[TheDialect, SnakeCase]] =
    new WithContextBase[TheDB, `2-comp-stereo-single`](TheDB, `2-comp-stereo-single`) {
      override type QuillContext = TheContext[SnakeCase] with `2-comp-stereo-single-lib`.public.PublicExtensions[TheDialect, SnakeCase]
      override protected def makeContext(ds: DataSource with Closeable) = new TheContext[SnakeCase](SnakeCase, ds) with `2-comp-stereo-single-lib`.public.PublicExtensions[TheDialect, SnakeCase]
    }

  implicit def mysqlContextForTest3: Aux[TheDB, `3-comp-stereo-oneschema`, TheContext[Literal] with `3-comp-stereo-oneschema-lib`.public.PublicExtensions[TheDialect, Literal]] =
    new WithContextBase[TheDB, `3-comp-stereo-oneschema`](TheDB, `3-comp-stereo-oneschema`) {
      override type QuillContext = TheContext[Literal] with `3-comp-stereo-oneschema-lib`.public.PublicExtensions[TheDialect, Literal]
      override protected def makeContext(ds: DataSource with Closeable) = new TheContext[Literal](Literal, ds) with `3-comp-stereo-oneschema-lib`.public.PublicExtensions[TheDialect, Literal]
    }

  implicit def mysqlContextForTest4: Aux[TheDB, `4-comp-stereo-twoschema`, TheContext[Literal] with `4-comp-stereo-twoschema-lib`.public.PublicExtensions[TheDialect, Literal] with `4-comp-stereo-twoschema-lib`.common.CommonExtensions[TheDialect, Literal]] = new WithContextBase[TheDB, `4-comp-stereo-twoschema`](TheDB, `4-comp-stereo-twoschema`) {
    override type QuillContext = TheContext[Literal] with `4-comp-stereo-twoschema-lib`.public.PublicExtensions[TheDialect, Literal] with `4-comp-stereo-twoschema-lib`.common.CommonExtensions[TheDialect, Literal]
    override protected def makeContext(ds: DataSource with Closeable) =
      new TheContext[Literal](Literal, ds) with `4-comp-stereo-twoschema-lib`.public.PublicExtensions[TheDialect, Literal] with `4-comp-stereo-twoschema-lib`.common.CommonExtensions[TheDialect, Literal]
  }

  implicit def mysqlContextForTest5: Aux[TheDB, `5-comp-non-stereo-allschema`, TheContext[Literal] with `5-comp-non-stereo-allschema-lib`.public.PublicExtensions[TheDialect, Literal] with `5-comp-non-stereo-allschema-lib`.alpha.AlphaExtensions[TheDialect, Literal] with `5-comp-non-stereo-allschema-lib`.bravo.BravoExtensions[TheDialect, Literal]] = new WithContextBase[TheDB, `5-comp-non-stereo-allschema`](TheDB, `5-comp-non-stereo-allschema`) {
    override type QuillContext = TheContext[Literal] with `5-comp-non-stereo-allschema-lib`.public.PublicExtensions[TheDialect, Literal] with `5-comp-non-stereo-allschema-lib`.alpha.AlphaExtensions[TheDialect, Literal] with `5-comp-non-stereo-allschema-lib`.bravo.BravoExtensions[TheDialect, Literal]
    override protected def makeContext(ds: DataSource with Closeable) =
      new TheContext[Literal](Literal, ds) with `5-comp-non-stereo-allschema-lib`.public.PublicExtensions[TheDialect, Literal] with `5-comp-non-stereo-allschema-lib`.alpha.AlphaExtensions[TheDialect, Literal] with `5-comp-non-stereo-allschema-lib`.bravo.BravoExtensions[TheDialect, Literal]
  }

}
