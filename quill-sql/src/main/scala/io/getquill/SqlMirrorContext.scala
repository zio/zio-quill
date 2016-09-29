package io.getquill

import io.getquill.idiom.{ Idiom => BaseIdiom }
import io.getquill.context.sql.SqlContext

class SqlMirrorContext[Idiom <: BaseIdiom, Naming <: NamingStrategy]
  extends MirrorContext[Idiom, Naming] with SqlContext[Idiom, Naming]