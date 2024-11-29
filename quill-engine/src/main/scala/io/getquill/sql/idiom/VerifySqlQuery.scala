package io.getquill.context.sql.idiom

import io.getquill.ast._
import io.getquill.context.sql._
import io.getquill.quotation.FreeVariables
import io.getquill.quat.Quat
import io.getquill.util.Text

case class Error(free: List[Ident], ast: Ast)
case class InvalidSqlQuery(errors: List[Error], query: SqlQuery) {
  override def toString = Text.JoinSynthesisError(errors, query)
}

class VerifySqlQuery(originalQuery: SqlQuery) {

  def verifyOrFail(): Option[String] =
    verify(originalQuery).map(_.toString)

  private def verify(query: SqlQuery): Option[InvalidSqlQuery] =
    query match {
      case q: FlattenSqlQuery             => verify(q)
      case SetOperationSqlQuery(a, op, b) => verify(a).orElse(verify(b))
      case UnaryOperationSqlQuery(op, q)  => verify(q)
    }

  private def verifyFlatJoins(q: FlattenSqlQuery) = {

    def loop(l: List[FromContext], available: Set[Ident]): Set[Ident] =
      l.foldLeft(available) {
        case (av, TableContext(_, alias)) => Set(alias)
        case (av, InfixContext(_, alias)) => Set(alias)
        case (av, QueryContext(_, alias)) => Set(alias)
        case (av, JoinContext(_, a, b, on)) =>
          av ++ loop(a :: Nil, av) ++ loop(b :: Nil, av)
        case (av, FlatJoinContext(_, a, on)) =>
          val nav     = av ++ loop(a :: Nil, av)
          val free    = FreeVariables(on)
          val invalid = free -- nav
          require(
            invalid.isEmpty,
            s"Found an `ON` table reference of a table that is not available: $invalid. " +
              "The `ON` condition can only use tables defined through explicit joins."
          )
          nav
      }
    loop(q.from, Set())
  }

  private def verify(query: FlattenSqlQuery): Option[InvalidSqlQuery] = {

    verifyFlatJoins(query)

    val aliases = query.from.flatMap(this.aliases) :+ Ident.trivial("*") :+ Ident.trivial("?")

    def verifyAst(ast: Ast) = {
      val freeVariables =
        (FreeVariables(ast) -- aliases).toList
      checkIllegalIdents(ast)
      freeVariables match {
        case Nil => None
        case free =>
          Some(
            Error(free.map(f => Ident(f.name, Quat.Value)), ast)
          ) // Quat is not actually needed here just for the sake of the Error Ident
      }
    }

    // Recursively expand children until values are fully flattened. Identities in all these should
    // be skipped during verification.
    def expandSelect(sv: SelectValue): List[SelectValue] =
      sv.ast match {
        case Tuple(values)        => values.map(v => SelectValue(v)).flatMap(expandSelect(_))
        case CaseClass(_, values) => values.map(v => SelectValue(v._2)).flatMap(expandSelect(_))
        case _                    => List(sv)
      }

    val freeVariableErrors: List[Error] =
      query.where.flatMap(verifyAst).toList ++
        query.orderBy.map(_.ast).flatMap(verifyAst) ++
        query.limit.flatMap(verifyAst) ++
        query.select
          .flatMap(expandSelect(_)) // Expand tuple select clauses so their top-level identities are skipped
          .map(_.ast)
          .filterNot(_.isInstanceOf[Ident])
          .flatMap(verifyAst) ++
        query.from.flatMap {
          case j: JoinContext     => verifyAst(j.on)
          case j: FlatJoinContext => verifyAst(j.on)
          case _                  => Nil
        }

    val nestedErrors =
      query.from.collect { case QueryContext(query, alias) =>
        verify(query).map(_.errors)
      }.flatten.flatten

    (freeVariableErrors ++ nestedErrors) match {
      case Nil    => None
      case errors => Some(InvalidSqlQuery(errors, originalQuery))
    }
  }

  private def aliases(s: FromContext): List[Ident] =
    s match {
      case s: TableContext    => List(s.alias)
      case s: QueryContext    => List(s.alias)
      case s: InfixContext    => List(s.alias)
      case s: JoinContext     => aliases(s.a) ++ aliases(s.b)
      case s: FlatJoinContext => aliases(s.a)
    }

  private def checkIllegalIdents(ast: Ast): Unit = {
    val freeIdents =
      (CollectAst(ast) {
        case op: OptionExists if op.quat.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use Option.exists on a table or embedded case class")
        case op: OptionForall if op.quat.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use Option.forAll on a table or embedded case class")
        case op: OptionGetOrElse if op.quat.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use Option.getOrElse on a table or embedded case class")
        case op: OptionIsEmpty if op.quat.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use Option.isEmpty on a table or embedded case class")
        case op: OptionNonEmpty if op.quat.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use Option.nonEmpty on a table or embedded case class")
        case op: OptionIsDefined if op.quat.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use Option.isDefined on a table or embedded case class")
        case op: OptionTableForall if op.quat.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use Option.tableForAll on a table or embedded case class")
        case op: OptionTableExists if op.quat.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use Option.tableExists on a table or embedded case class")

        case cond: If if cond.`then`.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use table or embedded case class as a result of a condition")
        case cond: If if cond.`else`.isInstanceOf[Quat.Product] =>
          throw new IllegalArgumentException("Cannot use table or embedded case class as a result of a condition")

        case cond: If => checkIllegalIdents(cond.condition)
        case other    => None
      })
  }
}

object VerifySqlQuery {
  def apply(query: SqlQuery) = new VerifySqlQuery(query)
}
