package io.getquill

final case class MappedEncoding[I, O](f: I => O)
