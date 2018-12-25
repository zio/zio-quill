package io.getquill.quotation

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context

import io.getquill.ast._
import io.getquill.util.Messages.RichContext
import io.getquill.norm.BetaReduction
import io.getquill.util.EnableReflectiveCalls

case class QuotedAst(ast: Ast) extends StaticAnnotation

trait Quotation extends Liftables with Unliftables with Parsing with ReifyLiftings {
  val c: Context
  import c.universe._

  private val quoted = TermName("quoted")

  def quote[T](body: Tree)(implicit t: WeakTypeTag[T]) = {

    val ast = BetaReduction(astParser(body))

    val id = TermName(s"id${ast.hashCode.abs}")

    val (reifiedAst, liftings) = reifyLiftings(ast)

    val quotation =
      c.untypecheck {
        q"""
          new ${c.prefix}.Quoted[$t] {
 
            ..${EnableReflectiveCalls(c)}
    
            @${c.weakTypeOf[QuotedAst]}($reifiedAst)
            def $quoted = ast
    
            override def ast = $reifiedAst
    
            def $id() = ()
            
            $liftings
          }
        """
      }

    if (IsDynamic(ast)) {
      q"$quotation: ${c.prefix}.Quoted[$t]"
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

  protected def unquote[T](tree: Tree)(implicit ct: ClassTag[T]) =
    astTree(tree).flatMap(astUnliftable.unapply).map {
      case ast: T => ast
    }

  private def astTree(tree: Tree) =
    for {
      method <- tree.tpe.decls.find(_.name == quoted)
      annotation <- method.annotations.headOption
      astTree <- annotation.tree.children.lastOption
    } yield astTree
}
