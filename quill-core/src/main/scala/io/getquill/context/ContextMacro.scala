package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.ast.{ Ast, Dynamic, Lift, Tag }
import io.getquill.quotation.{ IsDynamic, LiftUnlift, Quotation }
import io.getquill.util.LoadObject
import io.getquill.util.MacroContextExt._
import io.getquill.NamingStrategy
import io.getquill.idiom._
import io.getquill.quat.Quat

import scala.util.Success
import scala.util.Failure

trait ContextMacro extends Quotation {
  val c: MacroContext
  import c.universe.{ Function => _, Ident => _, _ }

  protected def expand(ast: Ast, topLevelQuat: Quat): Tree =
    q"""
      val (idiom, naming) = ${idiomAndNamingDynamic}
      val (ast, statement, executionType) = ${translate(ast, topLevelQuat)}
      io.getquill.context.Expand(${c.prefix}, ast, statement, idiom, naming, executionType)
    """

  protected def extractAst[T](quoted: Tree): Ast =
    unquote[Ast](c.typecheck(q"quote($quoted)"))
      .map(VerifyFreeVariables(c))
      .getOrElse {
        Dynamic(quoted)
      }

  private def translate(ast: Ast, topLevelQuat: Quat): Tree =
    IsDynamic(ast) match {
      case false => translateStatic(ast, topLevelQuat)
      case true  => translateDynamic(ast, topLevelQuat)
    }

  abstract class TokenLift(numQuatFields: Int) extends LiftUnlift(numQuatFields) {
    import mctx.universe.{ Ident => _, Constant => _, Function => _, If => _, _ }

    implicit val tokenLiftable: Liftable[Token] = Liftable[Token] {
      case ScalarTagToken(lift)       => q"io.getquill.idiom.ScalarTagToken(${lift: Tag})"
      case QuotationTagToken(lift)    => q"io.getquill.idiom.QuotationTagToken(${lift: Tag})"
      case StringToken(string)        => q"io.getquill.idiom.StringToken($string)"
      case ScalarLiftToken(lift)      => q"io.getquill.idiom.ScalarLiftToken(${lift: Lift})"
      case Statement(tokens)          => q"io.getquill.idiom.Statement(scala.List(..$tokens))"
      case SetContainsToken(a, op, b) => q"io.getquill.idiom.SetContainsToken($a, $op, $b)"
    }
  }

  private def translateStatic(ast: Ast, topLevelQuat: Quat): Tree = {
    val liftUnlift = new { override val mctx: c.type = c } with TokenLift(ast.countQuatFields)
    import liftUnlift._

    idiomAndNamingStatic match {
      case Success((idiom, naming)) =>
        val (normalizedAst, statement, _) = idiom.translate(ast, topLevelQuat, ExecutionType.Static)(naming)

        val (string, _) =
          ReifyStatement(
            idiom.liftingPlaceholder,
            idiom.emptySetContainsToken,
            statement,
            forProbing = true
          )

        ProbeStatement(idiom.prepareForProbing(string), c)

        c.query(string, idiom)

        q"($normalizedAst, ${statement: Token}, io.getquill.context.ExecutionType.Static)"
      case Failure(ex) =>
        c.info(s"Can't translate query at compile time because the idiom and/or the naming strategy aren't known at this point.")
        translateDynamic(ast, topLevelQuat)
    }
  }

  private def translateDynamic(ast: Ast, topLevelQuat: Quat): Tree = {
    val liftUnlift = new { override val mctx: c.type = c } with TokenLift(ast.countQuatFields)
    import liftUnlift._
    val liftQuat: Liftable[Quat] = liftUnlift.quatLiftable
    c.info("Dynamic query")
    val translateMethod = if (io.getquill.util.Messages.cacheDynamicQueries) {
      q"idiom.translateCached"
    } else q"idiom.translate"
    q"""
      val (idiom, naming) = ${idiomAndNamingDynamic}
      $translateMethod(_root_.io.getquill.norm.RepropagateQuats($ast), ${liftQuat(topLevelQuat)}, io.getquill.context.ExecutionType.Dynamic)(naming)
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
