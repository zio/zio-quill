package io.getquill.util

import scala.reflect.macros.whitebox.Context

object InferImplicitValueWithFallback {

  def apply(c: Context)(tpe: c.Type, fallbackTree: c.Tree) = {
    import c.universe._

    def fallback = {

      def fallbackImplicits =
        fallbackTree.tpe.members.collect {
          case m: MethodSymbol if (m.isImplicit && m.typeSignature.resultType.dealias <:< tpe) =>
            m
        }

      fallbackImplicits.headOption.map { m =>
        q"$fallbackTree.${m.name}"
      }
    }

    def inferImplicitValue(tpe: Type) =
      c.inferImplicitValue(tpe) match {
        case EmptyTree => None
        case value     => Some(value)
      }
    inferImplicitValue(tpe).orElse(fallback)
  }
}
