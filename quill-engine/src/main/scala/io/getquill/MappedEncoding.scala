package io.getquill

case class MappedEncoding[I, O](f: I => O)