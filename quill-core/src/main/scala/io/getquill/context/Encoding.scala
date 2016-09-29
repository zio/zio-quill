package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }

trait Decoder[R, T] {
  def apply(index: Int, row: R): T
}

object Encoding {

  def inferDecoder(c: MacroContext)(tpe: c.Type): Option[c.Tree] = {
    import c.universe._
    c.typecheck(q"implicitly[${c.prefix}.Decoder[$tpe]]", silent = true) match {
      case EmptyTree => None
      case tree      => Some(tree)
    }
  }
}
