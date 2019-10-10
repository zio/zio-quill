package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.ast.Ast
import io.getquill.ast.Dynamic
import io.getquill.quotation.Quotation
import io.getquill.util.LoadObject
import io.getquill.util.Messages._
import io.getquill.quotation.IsDynamic
import io.getquill.ast.Lift
import io.getquill.NamingStrategy
import io.getquill.idiom._

import scala.util.Success
import scala.util.Failure

trait ContextMacro extends Quotation {
  val c: MacroContext
  import c.universe.{ Function => _, Ident => _, _ }

  protected def expand(ast: Ast): Tree =
    q"""
      val (idiom, naming) = ${idiomAndNamingDynamic}
      val (ast, statement) = ${translate(ast)}
      io.getquill.context.Expand(${c.prefix}, ast, statement, idiom, naming)
    """

  protected def extractAst[T](quoted: Tree): Ast =
    unquote[Ast](c.typecheck(q"quote($quoted)"))
      .map(VerifyFreeVariables(c))
      .getOrElse {
        Dynamic(quoted)
      }

  private def translate(ast: Ast): Tree =
    IsDynamic(ast) match {
      case false => translateStatic(ast)
      case true  => translateDynamic(ast)
    }

  private implicit val tokenLiftable: Liftable[Token] = Liftable[Token] {
    case StringToken(string)        => q"io.getquill.idiom.StringToken($string)"
    case ScalarLiftToken(lift)      => q"io.getquill.idiom.ScalarLiftToken(${lift: Lift})"
    case Statement(tokens)          => q"io.getquill.idiom.Statement(scala.List(..$tokens))"
    case SetContainsToken(a, op, b) => q"io.getquill.idiom.SetContainsToken($a, $op, $b)"
  }

  private def translateStatic(ast: Ast): Tree = {
    idiomAndNamingStatic match {
      case Success((idiom, naming)) =>
        val (normalizedAst, statement) = idiom.translate(ast)(naming)

        val (string, _) =
          ReifyStatement(
            idiom.liftingPlaceholder,
            idiom.emptySetContainsToken,
            statement,
            forProbing = true
          )

        ProbeStatement(idiom.prepareForProbing(string), c)

        c.query(string, idiom)

        q"($normalizedAst, ${statement: Token})"
      case Failure(ex) =>
        c.info(s"Can't translate query at compile time because the idiom and/or the naming strategy aren't known at this point.")
        translateDynamic(ast)
    }
  }

  private def translateDynamic(ast: Ast): Tree = {
    c.info("Dynamic query")
    q"""
      val (idiom, naming) = ${idiomAndNamingDynamic}
      idiom.translate($ast)(naming)
    """
  }

  private def idiomAndNaming = {
    val (idiom :: n :: _) =
      c.prefix.actualType
        .baseType(c.weakTypeOf[Context[Idiom, NamingStrategy]].typeSymbol)
        .typeArgs
    (idiom, n)
  }

  private def idiomAndNamingDynamic =
    q"(${c.prefix}.idiom, ${c.prefix}.naming)"

  private def idiomAndNamingStatic = {
    val (idiom, naming) = idiomAndNaming
    for {
      idiom <- LoadObject[Idiom](c)(idiom)
      naming <- LoadNaming.static(c)(naming)
    } yield {
      (idiom, naming)
    }
  }
}
