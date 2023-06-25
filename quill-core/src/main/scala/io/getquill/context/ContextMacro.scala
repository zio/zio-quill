package io.getquill.context

import scala.reflect.macros.whitebox.{Context => MacroContext}
import io.getquill.ast.{Ast, Dynamic, Lift, Tag}
import io.getquill.quotation.{IsDynamic, LiftUnlift, Quotation}
import io.getquill.util.LoadObject
import io.getquill.util.MacroContextExt._
import io.getquill.{IdiomContext, NamingStrategy}
import io.getquill.idiom._
import io.getquill.quat.Quat
import scala.util.Failure
import scala.util.Success

trait ContextMacro extends Quotation {
  val c: MacroContext
  import c.universe.{Function => _, Ident => _, _}

  protected def expand(ast: Ast, topLevelQuat: Quat): Tree = {
    summonPhaseDisable()
    q"""
      val (idiom, naming) = ${idiomAndNamingDynamic}
      val (ast, statement, executionType, idiomContext) = ${translate(ast, topLevelQuat, None)}
      (idiomContext, io.getquill.context.Expand(${c.prefix}, ast, statement, idiom, naming, executionType))
    """
  }

  protected def extractAst[T](quoted: Tree): Ast =
    unquote[Ast](c.typecheck(q"quote($quoted)"))
      .map(VerifyFreeVariables(c))
      .getOrElse {
        Dynamic(quoted)
      }

  def translate(ast: Ast, topLevelQuat: Quat, batchAlias: Option[String]): Tree =
    IsDynamic(ast) match {
      case false => translateStatic(ast, topLevelQuat, batchAlias)
      case true  => translateDynamic(ast, topLevelQuat, batchAlias)
    }

  abstract class TokenLift(numQuatFields: Int) extends LiftUnlift(numQuatFields) {
    import mctx.universe.{Ident => _, Constant => _, Function => _, If => _, _}

    implicit val statementLiftable: Liftable[Statement] = Liftable[Statement] { case Statement(tokens) =>
      q"io.getquill.idiom.Statement(scala.List(..$tokens))"
    }

    implicit val tokenLiftable: Liftable[Token] = Liftable[Token] {
      case ScalarTagToken(lift)       => q"io.getquill.idiom.ScalarTagToken(${lift: Tag})"
      case QuotationTagToken(lift)    => q"io.getquill.idiom.QuotationTagToken(${lift: Tag})"
      case StringToken(string)        => q"io.getquill.idiom.StringToken($string)"
      case ScalarLiftToken(lift)      => q"io.getquill.idiom.ScalarLiftToken(${lift: Lift})"
      case s: Statement               => statementLiftable(s)
      case SetContainsToken(a, op, b) => q"io.getquill.idiom.SetContainsToken($a, $op, $b)"
      case ValuesClauseToken(stmt)    => q"io.getquill.idiom.ValuesClauseToken($stmt)"
    }
  }

  protected def tryTranslateStatic(
    ast: Ast,
    topLevelQuat: Quat,
    batchAlias: Option[String]
  ): Either[String, (Ast, Token, ExecutionType, IdiomContext, String, Idiom)] = {
    val transpileConfig = summonTranspileConfig()
    val queryType       = IdiomContext.QueryType.discoverFromAst(ast, batchAlias)
    val idiomContext    = IdiomContext(transpileConfig, queryType)

    idiomAndNamingStatic match {
      case Failure(_) =>
        Left(
          s"Can't translate query at compile time because the idiom and/or the naming strategy aren't known at this point."
        )
      case Success((idiom, naming)) =>
        try {
          val (normalizedAst, statement, _) =
            idiom.translate(ast, topLevelQuat, ExecutionType.Static, idiomContext)(naming)

          val (string, _) =
            ReifyStatement(
              idiom.liftingPlaceholder,
              idiom.emptySetContainsToken,
              statement,
              forProbing = true
            )

          ProbeStatement(idiom.prepareForProbing(string), c)
          c.query(string, idiom)
          Right(
            (normalizedAst, statement: Token, io.getquill.context.ExecutionType.Static, idiomContext, string, idiom)
          )
        } catch {
          case e: Exception =>
            c.fail(s"Query compilation failed. ${e.getMessage}")
        }
    }
  }

  private def translateStatic(ast: Ast, topLevelQuat: Quat, batchAlias: Option[String]): Tree = {
    val liftUnlift = new { override val mctx: c.type = c } with TokenLift(ast.countQuatFields)
    import liftUnlift._
    import ConfigLiftables._
    tryTranslateStatic(ast, topLevelQuat, batchAlias) match {
      case Right((normalizedAst, statement, executionType, idiomContext, _, _)) =>
        q"($normalizedAst, ${statement: Token}, ${executionType: ExecutionType}, $idiomContext)"

      case Left(msg) =>
        c.info(msg)
        translateDynamic(ast, topLevelQuat, batchAlias)
    }
  }

  private def translateDynamic(ast: Ast, topLevelQuat: Quat, batchAlias: Option[String]): Tree = {
    val liftUnlift = new { override val mctx: c.type = c } with TokenLift(ast.countQuatFields)
    import liftUnlift._
    val liftQuat: Liftable[Quat] = liftUnlift.quatLiftable
    val transpileConfig          = summonTranspileConfig()
    val transpileConfigExpr      = ConfigLiftables.transpileConfigLiftable(transpileConfig)
    // Compile-time AST might have Dynamic parts, we need those resolved (i.e. at runtime to be able to get the query type)
    val queryTypeExpr = q"_root_.io.getquill.IdiomContext.QueryType.discoverFromAst($ast, $batchAlias)"
    c.info("Dynamic query")
    val translateMethod = if (io.getquill.util.Messages.cacheDynamicQueries) {
      q"idiom.translateCached"
    } else q"idiom.translate"
    // The `idiomContext` variable uses scala's provided list-liftable and the optionalPhaseLiftable
    q"""
      val (idiom, naming) = ${idiomAndNamingDynamic}
      val traceConfig = ${ConfigLiftables.traceConfigLiftable(transpileConfig.traceConfig)}
      val idiomContext = _root_.io.getquill.IdiomContext($transpileConfigExpr, $queryTypeExpr)
      val (ast, statement, executionType) = $translateMethod(new _root_.io.getquill.norm.RepropagateQuats(traceConfig)($ast), ${liftQuat(
        topLevelQuat
      )}, io.getquill.context.ExecutionType.Dynamic, idiomContext)(naming)
      (ast, statement, executionType, idiomContext)
    """
  }

  private def idiomAndNaming = {
    val (idiom :: n :: _) =
      c.prefix.actualType
        .baseType(c.weakTypeOf[Context[Idiom, NamingStrategy]].typeSymbol)
        .typeArgs
    (idiom, n)
  }

  def idiomAndNamingDynamic =
    q"(${c.prefix}.idiom, ${c.prefix}.naming)"

  private def idiomAndNamingStatic = {
    val (idiom, naming) = idiomAndNaming
    for {
      idiom  <- LoadObject[Idiom](c)(idiom)
      naming <- LoadNaming.static(c)(naming)
    } yield {
      (idiom, naming)
    }
  }
}
