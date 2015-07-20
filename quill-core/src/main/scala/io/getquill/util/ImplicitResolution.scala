package io.getquill.util

import scala.reflect.macros.whitebox.Context

trait ImplicitResolution {
  val c: Context
  import c.universe._

  def inferImplicitValueWithFallback[T](tpe: Type, fallbackType: Type, fallbackTree: Tree): Option[Tree] =
    inferImplicitValue(tpe).orElse {
      val members =
        fallbackType.members.collect {
          case m: MethodSymbol if (m.isImplicit && m.typeSignature.resultType.dealias =:= tpe) =>
            m
        }
      members.headOption.map { m =>
        q"$fallbackTree.${m.name}"
      }
    }

  private def inferImplicitValue(tpe: Type) =
    c.inferImplicitValue(tpe) match {
      case EmptyTree => None
      case value     => Some(value)
    }
}