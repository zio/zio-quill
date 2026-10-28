package zio.quill

import zio.quill.idiom.{Idiom => BaseIdiom}
import zio.quill.context.sql.SqlContext
import zio.quill.context.sql.encoding.mirror.ArrayMirrorEncoding

class SqlMirrorContext[+Idiom <: BaseIdiom, +Naming <: NamingStrategy](idiom: Idiom, naming: Naming)
    extends MirrorContext(idiom, naming)
    with SqlContext[Idiom, Naming]
    with ArrayMirrorEncoding
