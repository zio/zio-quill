package zio.quill

import zio.quill.context.qzio.ImplicitSyntax.Implicit

package object misc {
  implicit val pool = Implicit(zio.quill.postgres.pool)
  object testContext extends PostgresZioJdbcContext(Literal) with TestEntities
}
