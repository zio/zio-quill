package io.getquill.dsl.macroz

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.{ Context => MacroContext }

trait ExpandActionMacro {
  val c: MacroContext
  import c.universe._

  def expandInsert[T](value: Tree): Tree =
    expandAction(value, "insert")

  def expandUpdate[T](value: Tree): Tree =
    expandAction(value, "update")

  private def expandAction(value: Tree, method: String) = {
    val assignments = expandAssignments(value, value.tpe)
    q"${c.prefix}.${TermName(method)}(..$assignments)"
  }

  private def expandAssignments[T](value: Tree, tpe: Type): List[Tree] =
    caseClassConstructor(tpe) match {
      case None => c.fail("Can't expand a non-case class")
      case Some(constructor) =>
        constructor.paramLists.flatten.map { param =>
          val term = param.name.toTermName
          q"(v: $tpe) => v.$term -> $value.$term"
        }
    }

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if (m.isPrimaryConstructor) => m
    }.headOption
}