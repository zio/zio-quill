package io.getquill

import fansi.Str
import io.getquill.ast.Renameable.{ ByStrategy, Fixed }
import io.getquill.ast.Visibility.{ Hidden, Visible }
import io.getquill.ast._
import io.getquill.quat.Quat
import io.getquill.util.Messages.QuatTrace
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

class AstPrinter(traceOpinions: Boolean, traceAstSimple: Boolean, traceQuats: QuatTrace) extends pprint.Walker {
  val defaultWidth: Int = 150
  val defaultHeight: Int = Integer.MAX_VALUE
  val defaultIndent: Int = 2
  val colorLiteral: fansi.Attrs = fansi.Color.Green
  val colorApplyPrefix: fansi.Attrs = fansi.Color.Yellow
  def escapeUnicode = false
  def showFieldNames = false

  val traceAllQuats = traceQuats == QuatTrace.All

  private def printRenameable(r: Renameable) =
    r match {
      case ByStrategy => Tree.Literal("Ren")
      case Fixed      => Tree.Literal("Fix")
    }

  override def additionalHandlers: PartialFunction[Any, Tree] = PartialFunction.empty

  private def printVisibility(v: Visibility) =
    v match {
      case Visible => Tree.Literal("Vis")
      case Hidden  => Tree.Literal("Hide")
    }

  private trait treemake {
    private def toContent =
      this match {
        case q: treemake.Quat    => treemake.Content(List(q))
        case e: treemake.Elem    => treemake.Content(List(e))
        case e: treemake.Tree    => treemake.Content(List(e))
        case c: treemake.Content => c
      }
    def withQuat(q: Quat): treemake = toContent.andWith(treemake.Quat(q))
    def withTree(t: pprint.Tree): treemake = toContent.andWith(treemake.Tree(t))
    private def treeifyList: List[Tree] =
      toContent.list.flatMap {
        case e: treemake.Quat =>
          traceQuats match {
            case QuatTrace.Full | QuatTrace.All => List(Tree.Literal(e.q.shortString))
            case QuatTrace.Short                => List(Tree.Literal(e.q.shortString.take(10)))
            case QuatTrace.None                 => List()
          }
        case treemake.Elem(value)   => List(pprint.treeify(value, escapeUnicode, showFieldNames))
        case treemake.Tree(value)   => List(value)
        case treemake.Content(list) => list.flatMap(_.treeifyList)
      }
    def treeify: Iterator[Tree] = treeifyList.iterator
  }
  private object treemake {
    private case class Quat(q: io.getquill.quat.Quat) extends treemake
    private case class Elem(any: Any) extends treemake
    private case class Tree(any: pprint.Tree) extends treemake
    private case class Content(list: List[treemake]) extends treemake {
      def andWith(elem: treemake) =
        elem match {
          case c: Content => Content(list ++ c.list)
          case other      => Content(list :+ other)
        }
    }

    def apply(list: Any*): treemake = Content(list.toList.map(Elem(_)))
  }

  override def treeify(x: Any, escapeUnicode: Boolean, showFieldNames: Boolean): Tree =
    x match {
      case ast: Ast if (traceAstSimple) =>
        Tree.Literal("" + ast) // Do not blow up if it is null

      case past: PseudoAst if (traceAstSimple) =>
        Tree.Literal("" + past) // Do not blow up if it is null

      case i: Ident =>
        Tree.Apply("Id", treemake(i.name).withQuat(i.bestQuat).treeify)

      case e: Entity if (!traceOpinions) =>
        Tree.Apply("Entity", treemake(e.name, e.properties).withQuat(e.bestQuat).treeify)

      case q: Quat            => Tree.Literal(q.shortString)

      case s: ScalarValueLift => Tree.Apply("ScalarValueLift", treemake("..." + s.name.reverse.take(15).reverse).withQuat(s.bestQuat).treeify)

      case p: Property if (traceOpinions) =>
        TreeApplyList("Property", l(treeify(p.ast, escapeUnicode, showFieldNames)) ++ l(treeify(p.name, escapeUnicode, showFieldNames)) ++
          (
            if (traceOpinions)
              l(printRenameable(p.renameable), printVisibility(p.visibility))
            else
              List.empty[Tree]
          ) ++
            (
              if (traceAllQuats)
                l(treeify(p.bestQuat, escapeUnicode, showFieldNames))
              else
                List.empty[Tree]
            ))

      case e: Entity if (traceOpinions) =>
        Tree.Apply("Entity", treemake(e.name, e.properties).withTree(printRenameable(e.renameable)).withQuat(e.bestQuat).treeify)

      case ast: Ast =>
        if (traceAllQuats)
          super.treeify(ast, escapeUnicode, showFieldNames) match {
            case Tree.Apply(prefix, body) =>
              TreeApplyList(prefix, body.toList :+ treeify(ast.bestQuat, escapeUnicode, showFieldNames))
            case other => other
          }
        else
          super.treeify(ast, escapeUnicode, showFieldNames)

      case _ => super.treeify(x, escapeUnicode, showFieldNames)
    }

  private def TreeApplyList(prefix: String, body: List[Tree]) = Tree.Apply(prefix, body.iterator)

  private def l(trees: Tree*): List[Tree] = List[Tree](trees: _*)

  def apply(x: Any): fansi.Str = {
    fansi.Str.join(this.tokenize(x).toSeq)
  }

  def tokenize(x: Any): Iterator[fansi.Str] = {
    val tree = this.treeify(x, escapeUnicode, showFieldNames)
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
