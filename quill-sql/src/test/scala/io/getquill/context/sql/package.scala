package io.getquill.context

import io.getquill.naming.Literal
import io.getquill.context.sql.mirror.SqlMirrorContext

package object sql {
  val mirrorContext = new SqlMirrorContext[Literal]
}

