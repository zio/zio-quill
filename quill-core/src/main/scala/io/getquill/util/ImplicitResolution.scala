package io.getquill.util

import scala.reflect.macros.whitebox.Context

trait ImplicitResolution extends Messages {
  val c: Context
  import c.universe._

  def inferImplicitValueWithFallback[T](tpe: Type, fallbackType: Type, fallbackTree: Tree): Option[Tree] =
    inferImplicitValue(tpe).orElse(fallback(tpe, fallbackType, fallbackTree))

  private def fallback(tpe: Type, fallbackType: Type, fallbackTree: Tree) =
    fallbackImplicit(tpe, fallbackType).map { m =>
      q"$fallbackTree.${m.name}"
    }

  private def fallbackImplicit(tpe: Type, fallbackType: Type) =
    fallbackImplicits(tpe, fallbackType).toList match {
      case Nil          => None
      case value :: Nil => Some(value)
      case multiple     => fail(s"Multiple implicits found for '$tpe' at '$fallbackType")
    }

  private def fallbackImplicits(tpe: Type, fallbackType: Type) =
    fallbackType.members.collect {
      case m: MethodSymbol if (m.isImplicit && m.typeSignature.resultType.dealias <:< tpe) =>
        m
    }

  private def inferImplicitValue(tpe: Type) =
    c.inferImplicitValue(tpe) match {
      case EmptyTree => None
      case value     => Some(value)
    }
}