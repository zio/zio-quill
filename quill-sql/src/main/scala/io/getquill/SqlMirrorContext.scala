package io.getquill

import io.getquill.idiom.{ Idiom => BaseIdiom }
import io.getquill.context.sql.SqlContext
import io.getquill.context.sql.encoding.mirror.ArrayMirrorEncoding

class SqlMirrorContext[Idiom <: BaseIdiom, Naming <: NamingStrategy](idiom: Idiom, naming: Naming)
  extends MirrorContext(idiom, naming)
  with SqlContext[Idiom, Naming]
  with ArrayMirrorEncoding