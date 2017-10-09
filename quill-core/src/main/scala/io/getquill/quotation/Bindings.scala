package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

object Bindings {
  def apply(c: Context)(quoted: c.Tree, tpe: c.Type): Map[c.Symbol, c.Tree] = {
    import c.universe._
    tpe
      .member(TermName("bindings"))
      .typeSignature.decls.collect {
        case m: MethodSymbol if (m.isGetter) =>
          m ->
            q"""
              {
                import _root_.scala.language.reflectiveCalls
                $quoted.bindings.$m
              }
            """
      }.toMap
  }
}
