package io.getquill.util

import scala.reflect.macros.whitebox.Context

import Messages.RichContext

object InferImplicitValueWithFallback {

  def apply(c: Context)(tpe: c.Type, fallbackTree: c.Tree) = {
    import c.universe._

    def fallback = {

      def fallbackImplicits =
        fallbackTree.tpe.members.collect {
          case m: MethodSymbol if (m.isImplicit && m.typeSignature.resultType.dealias <:< tpe) =>
            m
        }

      def fallbackImplicit =
        fallbackImplicits.toList match {
          case Nil          => None
          case value :: Nil => Some(value)
          case multiple =>
            c.fail(s"Multiple implicits '$multiple' found for '$tpe' at '$fallbackTree")
        }

      fallbackImplicit.map { m =>
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
