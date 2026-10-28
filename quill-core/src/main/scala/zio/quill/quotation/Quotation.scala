package zio.quill.quotation

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context
import zio.quill.ast._
import zio.quill.util.MacroContextExt._
import zio.quill.norm.BetaReduction
import zio.quill.util.Messages.TraceType
import zio.quill.util.{EnableReflectiveCalls, Interpolator, Messages}

case class QuotedAst(ast: Ast) extends StaticAnnotation

abstract class LiftUnlift(numQuatFields: Int) extends Liftables with Unliftables {
  lazy val serializeQuats: Boolean = numQuatFields > Messages.maxQuatFields
}

trait Quotation extends Parsing with ReifyLiftings {
  val c: Context
  import c.universe._

  private val quoted = TermName("quoted")

  def quote[T](body: Tree)(implicit t: WeakTypeTag[T]) = {
    val interp = new Interpolator(TraceType.Quotation, transpileConfig.traceConfig, 1)
    import interp._

    val ast = BetaReduction(trace"Parsing Quotation Body" andReturn (astParser(body)))

    val id                     = TermName(s"id${ast.hashCode.abs}")
    val (reifiedAst, liftings) = reifyLiftings(ast)

    val liftUnlift = new { override val mctx: c.type = c } with LiftUnlift(reifiedAst.countQuatFields)

    // Technically can just put reifiedAst into the quasi-quote directly but this is more comprehensible
    val liftedAst: c.Tree = liftUnlift.astLiftable(reifiedAst)

    val quotation =
      c.untypecheck {
        q"""
          new zio.quill.Quoted[$t] {

            ..${EnableReflectiveCalls(c)}

            @${c.weakTypeOf[QuotedAst]}($liftedAst)
            def $quoted = ast

            override def ast = $liftedAst

            def $id() = ()

            $liftings
          }
        """
      }

    if (IsDynamic(ast)) {
      q"$quotation: zio.quill.Quoted[$t]"
    } else {
      quotation
    }
  }

  def doubleQuote[T: WeakTypeTag](body: Expr[Any]) =
    body.tree match {
      case q"null" => c.fail("Can't quote null")
      case tree    => q"${c.prefix}.unquote($tree)"
    }

  def quotedFunctionBody(func: Expr[Any]) =
    func.tree match {
      case q"(..$p) => $b" => q"${c.prefix}.quote((..$p) => ${c.prefix}.unquote($b))"
    }

  protected def unquote[T](tree: Tree)(implicit ct: ClassTag[T]) = {
    val unlift = new { override val mctx: c.type = c } with Unliftables
    import unlift._
    astTree(tree).flatMap(astUnliftable.unapply).map { case ast: T =>
      ast
    }
  }

  private def astTree(tree: Tree) =
    for {
      method     <- tree.tpe.decls.find(_.name == quoted)
      annotation <- method.annotations.headOption
      astTree    <- annotation.tree.children.lastOption
    } yield astTree

  def makeQuat[T: c.WeakTypeTag]: c.Tree = {
    val quat             = inferQuat(implicitly[c.WeakTypeTag[T]].tpe)
    val liftUnlift       = new { override val mctx: c.type = c } with LiftUnlift(quat.countFields)
    val quatExpr: c.Tree = liftUnlift.quatLiftable(quat)
    q"${quatExpr}"
  }
}
