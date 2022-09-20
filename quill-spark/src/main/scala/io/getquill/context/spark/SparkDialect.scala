package io.getquill.context.spark

import io.getquill.{IdiomContext, NamingStrategy}
import io.getquill.ast.{
  Ast,
  BinaryOperation,
  CaseClass,
  Constant,
  ExternalIdent,
  Ident,
  Operation,
  Property,
  Query,
  StringOperator,
  Tuple,
  Value
}
import io.getquill.context.spark.norm.EscapeQuestionMarks
import io.getquill.context.sql.{
  FlattenSqlQuery,
  SelectValue,
  SetOperationSqlQuery,
  SqlQuery,
  SqlQueryApply,
  UnaryOperationSqlQuery
}
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.norm.SqlNormalize
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Token
import io.getquill.util.Messages.trace
import io.getquill.context.{CannotReturn, ExecutionType}
import io.getquill.quat.Quat

class SparkDialect extends SparkIdiom

trait SparkIdiom extends SqlIdiom with CannotReturn { self =>

  def parentTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy, idiomContext: IdiomContext) =
    super.sqlQueryTokenizer

  def liftingPlaceholder(index: Int): String = "?"

  override def prepareForProbing(string: String) = string

  override implicit def externalIdentTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy
  ): Tokenizer[ExternalIdent] = super.externalIdentTokenizer

  override def translate(ast: Ast, topLevelQuat: Quat, executionType: ExecutionType, idiomContext: IdiomContext)(
    implicit naming: NamingStrategy
  ) = {
    val normalizedAst = EscapeQuestionMarks(SqlNormalize(ast, idiomContext.config))

    implicit val implicitIdiomContext: IdiomContext = idiomContext
    implicit val tokernizer                         = defaultTokenizer

    val token =
      normalizedAst match {
        case q: Query =>
          val sql = new SqlQueryApply(idiomContext.config.traceConfig)(q)
          trace("sql")(sql)
          val expanded = SimpleNestedExpansion(sql)
          trace("expanded sql")(expanded)
          val tokenized = expanded.token
          trace("tokenized sql")(tokenized.toString)
          tokenized
        case other =>
          other.token
      }

    (normalizedAst, stmt"$token", executionType)
  }

  override def concatFunction = "explode"

  override implicit def identTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy
  ): Tokenizer[Ident] = Tokenizer[Ident] {
    case id @ Ident(name, q @ Quat.Product(fields)) if (q.tpe == Quat.Product.Type.Concrete) =>
      stmt"struct(${fields.map { case (field, subQuat) => (Property(id, field): Ast) }.toList.token})"
    case id @ Ident(name, q: Quat.Product) if (q.tpe == Quat.Product.Type.Abstract) =>
      stmt"struct(${name.token}.*)"
    // Situations where a single ident arise with is a Quat.Value typically only happen when an operation yields a single SelectValue
    // e.g. a concatMap (or aggregation?)
    case Ident(name, Quat.Value) =>
      stmt"${name.token}.x"
    case Ident(name, _) =>
      stmt"${name.token}"
  }

  class SparkFlattenSqlQueryTokenizerHelper(q: FlattenSqlQuery)(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ) extends FlattenSqlQueryTokenizerHelper(q)(astTokenizer, strategy, idiomContext) {

    override def selectTokenizer: Token =
      // Note that by the time we have reached this point, all Idents representing case classes/tuples in selection have
      // already been expanded into their composite properties. This is handled via the select.token which delegates
      // to the list tokenizer which delegates to the select value tokenizer. The remaining cases that need special handling
      // are when there is a single select-value which represents an Ident that cannot be expanded
      // (either because it's quat is a placeholder type, or it's quat is abstract), or a single
      // value-level entity (either a Ident whose quat is Quat.Value) or is a single-value field
      // e.g. a Property, Constant, etc... as defined by SingleValuePrimitive.
      // The logic of these cases is simple. Any Ident which whose type is unknown needs to be star-expanded, there
      // is no other choice since we don't know anything else about it. Any primitive-valued ident also needs to be
      // star-expanded since we do not know the alias of the actual primitive field (hopefully it is 'single' as defined
      // by SimpleNestedExpansion but we cannot know for sure).
      // For any other kind of entity e.g. a Property, etc... we can safely tokenize the select-value whose alias
      // should hopefully also be 'single'
      q.select match {
        case Nil => stmt"*"
        // If we have encountered a situation where a abstract ident is on the top level and the only select field, (i.e. select.length == 1)
        // it is a strange situation that needs special handling.
        // We cannot expand the fields of the ident because there might be fields that we don't know about. We also can't just pass
        // the ident along since in the SparkDialect it will be rendered as Ident("a") => struct(a.*) which doesn't work in a top-level select
        // (i.e. you can't do "select struct(a.*) from ..." since on the top level the fields must be expanded.)
        // The only good option that I have thought of so far, is to expand the Ident in the SparkDialect directly
        // in the FlattenSqlTokenizer with special handling for length=1 selects
        case List(SelectValue(Ident(a, q @ Quat.Product(_)), _, _)) if (q.tpe == Quat.Product.Type.Abstract) =>
          stmt"${a.token}.*"
        // it is an ident but somehow it's type is not known
        case List(SelectValue(Ident(a, Quat.Placeholder(_)), _, _)) =>
          stmt"${a.token}.*"
        // It is an ident but actually it represents a single sql-level value
        case List(SelectValue(Ident(a, _: Quat.Primitive), _, _)) =>
          stmt"${a.token}.*"
        // If the selection is a single value e.g. SelectValue(prop.value), SelectValue(Constant) return it right here as a SingleValuePrimitive
        case sv @ List(SelectValue(a @ SingleValuePrimitive(), _, _)) =>
          sv.token
        // Otherwise it should have multiple values.
        // TODO Maybe we should even introduce an exception here because all the support single-value selects have already been enumerated
        case _ => q.select.token
      }

  }

  override implicit def sqlQueryTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[SqlQuery] = Tokenizer[SqlQuery] {
    case q: FlattenSqlQuery =>
      new SparkFlattenSqlQueryTokenizerHelper(q).apply
    case SetOperationSqlQuery(a, op, b) =>
      stmt"(${a.token}) ${op.token} (${b.token})"
    case UnaryOperationSqlQuery(op, q) =>
      stmt"SELECT ${op.token} (${q.token})"
  }

  override implicit def propertyTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy
  ): Tokenizer[Property] = {
    def path(ast: Ast): Token =
      ast match {
        case Ident(name, _) => name.token
        case Property.Opinionated(a, b, renameable, _) =>
          stmt"${path(a)}.${renameable.fixedOr(b.token)(strategy.column(b).token)}"
        case other =>
          other.token
      }
    Tokenizer[Property] { case p =>
      path(p).token
    }
  }

  override implicit def operationTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy
  ): Tokenizer[Operation] = Tokenizer[Operation] {
    case BinaryOperation(a, StringOperator.`+`, b) => stmt"concat(${a.token}, ${b.token})"
    case op                                        => super.operationTokenizer.token(op)
  }

  override implicit def valueTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy
  ): Tokenizer[Value] = Tokenizer[Value] {
    case Constant(v: String, _) => stmt"'${v.replaceAll("""[\\']""", """\\$0""").token}'"
    case Tuple(values) =>
      stmt"struct(${values.zipWithIndex.map { case (value, index) =>
          stmt"${value.token} AS _${(index + 1).toString.token}"
        }.token})"
    case CaseClass(_, values) =>
      stmt"struct(${values.map { case (name, value) => stmt"${value.token} AS ${name.token}" }.token})"
    case other => super.valueTokenizer.token(other)
  }

  override protected def tokenizeGroupBy(
    values: Ast
  )(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Token =
    values match {
      case Tuple(items) => items.mkStmt()
      case values       => values.token
    }
}

object SparkDialect extends SparkDialect
