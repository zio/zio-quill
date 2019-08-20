package io.getquill

import io.getquill.ast.Renameable.{ ByStrategy, Fixed }
import io.getquill.ast.{ Entity, Property, Renameable }
import pprint.{ Renderer, Tree, Truncated }

class AstPrinter(traceOpinions: Boolean) extends pprint.Walker {
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

  override def additionalHandlers: PartialFunction[Any, Tree] = {
    case p: Property if (traceOpinions) =>
      Tree.Apply("Property", List[Tree](treeify(p.ast), treeify(p.name), printRenameable(p.renameable)).iterator)

    case e: Entity if (traceOpinions) =>
      Tree.Apply("Property", List[Tree](treeify(e.name), treeify(e.properties), printRenameable(e.renameable)).iterator)
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