package zio.quill.context.sql.idiom

import zio.quill.util.Messages

trait ConcatSupport {
  this: SqlIdiom =>

  override def concatFunction = "UNNEST"
}

trait NoConcatSupport {
  this: SqlIdiom =>

  override def concatFunction = Messages.fail(s"`concatMap` not supported by ${this.getClass.getSimpleName}")
}
