package io.getquill.context

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.CompositeContext
import io.getquill.idiom.Idiom
import io.getquill.NamingStrategy

object CompositeContextMacro {
  def contextPrefix(c: MacroContext) = {
    import c.universe._
    c.debug(c.openMacros.last.prefix)
    c.prefix.actualType.member(TermName("contexts")) match {
      case NoSymbol => ""
      case _ =>
        val tpe =
          c.prefix.actualType
            .typeSymbol.info.toString
            .replaceAllLiterally("io.getquill.", "")
            .split('{')(0)
        s"$tpe: "
    }
  }
}

class CompositeContextMacro(val c: MacroContext) {
  import c.universe._

  def run(quoted: Tree): Tree = {
    val elems =
      c.prefix.actualType.member(TermName("contexts")).typeSignature.decls.toList
    val calls =
      elems.map { e =>
        cq"""
          _ if($e.cond()) => $e.ctx.run($quoted)
        """
      }
    q"""
      () match {
        case ..$calls
        case _ =>
          io.getquill.util.Messages.fail("Can't find an enabled context.")
      }
    """
        ???
  }

  def apply(elements: Tree*): Tree = {
    val vals =
      elements.zipWithIndex.map {
        case (e, i) => q"val ${TermName(s"_$i")} = $e"
      }
    c.debug {
    q"""
      import scala.language.experimental.macros
      new io.getquill.dsl.CoreDsl {

        override def run[T](quoted: Quoted[Query[T]]): Result[RunQueryResult[T]] = macro io.getquill.context.CompositeContextMacro.run

        object contexts {
          ..$vals
        }
      }
    """
    }
  }
}
