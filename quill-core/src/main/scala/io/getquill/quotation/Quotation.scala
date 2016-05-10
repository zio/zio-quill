package io.getquill.quotation

import io.getquill.util.Messages._

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context
import io.getquill.ast._

import scala.reflect.NameTransformer

trait Quoted[+T] {
  def ast: Ast
}

case class QuotedAst(ast: Ast) extends StaticAnnotation

trait Quotation extends Liftables with Unliftables with Parsing {

  val c: Context
  import c.universe._

  def quote[T: WeakTypeTag](body: Expr[T]) = {

    def bindingName(s: String) =
      TermName(NameTransformer.encode(s))

    val ast =
      Transform(astParser(body.tree)) {
        case QuotedReference(nested: Tree, nestedAst) =>
          val ast =
            Transform(nestedAst) {
              case RuntimeBinding(name) =>
                RuntimeBinding(s"$nested.$name")
            }
          QuotedReference(nested, ast)
      }

    val nestedBindings =
      CollectAst(ast) {
        case QuotedReference(nested: Tree, nestedAst) =>
          Bindings(c)(nested, nested.tpe).map {
            case (symbol, tree) =>
              val nestedName = bindingName(s"$nested.${symbol.name.decodedName}")
              q"val $nestedName = $tree"
          }
      }.flatten

    val bindings =
      CollectAst(ast) {
        case CompileTimeBinding(tree: Tree) =>
          q"val ${bindingName(tree.toString)} = $tree"
      }

    val id = TermName(s"id${ast.hashCode}")

    val reifiedAst =
      Transform(ast) {
        case CompileTimeBinding(tree: Tree) =>
          Dynamic(tree)
      }

    val quotation =
      q"""
        {
          new ${c.weakTypeOf[Quoted[T]]} { 
    
            @${c.weakTypeOf[QuotedAst]}($ast)
            def quoted = ast
    
            override def ast = $reifiedAst
            override def toString = ast.toString
    
            def $id() = ()
            
            val bindings = new {
              ..$bindings
              ..$nestedBindings
            }
          }
        }
      """

    IsDynamic(ast) match {
      case true  => q"$quotation: ${c.weakTypeOf[Quoted[T]]}"
      case false => quotation
    }
  }

  def doubleQuote[T: WeakTypeTag](body: Expr[Quoted[T]]) =
    body.tree match {
      case q"null" => c.fail("Can't quote null")
      case tree    => q"io.getquill.unquote($tree)"
    }

  def quotedFunctionBody(func: Expr[Any]) =
    func.tree match {
      case q"(..$p) => $b" => q"io.getquill.quote((..$p) => io.getquill.unquote($b))"
    }

  protected def unquote[T](tree: Tree)(implicit ct: ClassTag[T]) =
    astTree(tree).flatMap(astUnliftable.unapply).map {
      case ast: T => ast
    }

  private def astTree(tree: Tree) =
    for {
      method <- tree.tpe.decls.find(_.name.decodedName.toString == "quoted")
      annotation <- method.annotations.headOption
      astTree <- annotation.tree.children.lastOption
    } yield (astTree)
}
