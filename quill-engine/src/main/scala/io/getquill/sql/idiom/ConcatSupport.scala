package io.getquill.context.sql.idiom

import io.getquill.util.Messages

trait ConcatSupport {
  this: SqlIdiom =>

  override def concatFunction = "UNNEST"
}

trait NoConcatSupport {
  this: SqlIdiom =>

  override def concatFunction: Nothing = Messages.fail(s"`concatMap` not supported by ${this.getClass.getSimpleName}")
}
