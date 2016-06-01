package io.getquill.dsl

private[dsl] trait EncondingDsl {

  case class MappedEncoding[I, O](f: I => O)

  def mappedEncoding[I, O](f: I => O) = MappedEncoding(f)
}
