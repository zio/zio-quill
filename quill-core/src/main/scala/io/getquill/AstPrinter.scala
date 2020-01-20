package io.getquill

import fansi.Str
import io.getquill.ast.Renameable.{ ByStrategy, Fixed }
import io.getquill.ast.Visibility.{ Hidden, Visible }
import io.getquill.ast._
import pprint.{ Renderer, Tree, Truncated }

object AstPrinter {
  object Implicits {
    implicit class FansiStrExt(str: Str) {
      def string(color: Boolean): String =
        if (color) str.render
        else str.plainText
    }
  }
}

class AstPrinter(traceOpinions: Boolean, traceAstSimple: Boolean) extends pprint.Walker {
  val defaultWidth: Int = 150
  val defaultHeight: Int = Integer.MAX_VALUE
  val defaultIndent: Int = 2
  val colorLiteral: fansi.Attrs = fansi.Color.Green
  val colorApplyPrefix: fansi.Attrs = fansi.Color.Yellow

  private def printRenameable(r: Renameable) =
    r match {
      case ByStrategy => Tree.Literal("S")
      case Fixed      => Tree.Literal("F")
    }

  private def printVisibility(v: Visibility) =
    v match {
      case Visible => Tree.Literal("Vis")
      case Hidden  => Tree.Literal("Hide")
    }

  override def additionalHandlers: PartialFunction[Any, Tree] = {
    case ast: Ast if (traceAstSimple) =>
      Tree.Literal("" + ast) // Do not blow up if it is null

    case past: PseudoAst if (traceAstSimple) =>
      Tree.Literal("" + past) // Do not blow up if it is null

    case p: Property if (traceOpinions) =>
      Tree.Apply("Property", List[Tree](treeify(p.ast), treeify(p.name), printRenameable(p.renameable), printVisibility(p.visibility)).iterator)

    case e: Entity if (traceOpinions) =>
      Tree.Apply("Entity", List[Tree](treeify(e.name), treeify(e.properties), printRenameable(e.renameable)).iterator)
  }

  def apply(x: Any): fansi.Str = {
    fansi.Str.join(this.tokenize(x).toSeq: _*)
  }

  def tokenize(x: Any): Iterator[fansi.Str] = {
    val tree = this.treeify(x)
    val renderer = new Renderer(defaultWidth, colorApplyPrefix, colorLiteral, defaultIndent)
    val rendered = renderer.rec(tree, 0, 0).iter
    val truncated = new Truncated(rendered, defaultWidth, defaultHeight)
    truncated
  }
}

/**
 * A trait to be used by elements that are not proper AST elements but should still be treated as though
 * they were in the case where `traceAstSimple` is enabled (i.e. their toString method should be
 * used instead of the standard qprint AST printing)
 */
trait PseudoAst
