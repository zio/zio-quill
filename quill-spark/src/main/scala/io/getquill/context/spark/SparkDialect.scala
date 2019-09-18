package io.getquill.context.spark

import io.getquill.NamingStrategy
import io.getquill.ast.Ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Ident
import io.getquill.ast.Operation
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.StringOperator
import io.getquill.ast.Tuple
import io.getquill.ast.Value
import io.getquill.ast.CaseClass
import io.getquill.context.spark.norm.EscapeQuestionMarks
import io.getquill.context.sql.{ FlattenSqlQuery, SelectValue, SqlQuery }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.norm.SqlNormalize
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Token
import io.getquill.util.Messages.trace
import io.getquill.ast.Constant
import io.getquill.context.CannotReturn

class SparkDialect extends SparkIdiom

/**
 * This helper object is needed to instantiate SparkDialect instances that have multipleSelect enabled.
 * This is a simple alternative to query re-writing that allows Quill table aliases to be selected
 * in a away that Spark can understand them.
 *
 * Quill will represent table variable returns as identifiers e.g.
 * <br/><code>Query[Foo.join(Query[Bar]).on({case (f,b) => f.id == b.fk})</code>
 * <br/>will become:
 * <br/><code>select f.*, b.* from Foo f join Bar b on f.id == b.fk</code>
 * In order for this to work properly all we have to do is change the query to:
 * <br/><code>select struct(f.*), struct(b.*) from Foo f join Bar b on f.id == b.fk</code>
 *
 * Since the expression of the <code>f</code> and <code>b</code> identifiers in the query happens inside a tokenizer,
 * all that is needed for a tokenizer to be able the add the <code>struct</code> part is to introduce a <code>multipleSelect</code>
 * variable that will guide the expansion. The <code>SparkDialectRecursor</code> has been introduced specifically for this reason.
 * I.e. so that SparkDialect contexts can be recursively declared with a new <code>multipleSelect</code> value.
 *
 * Multiple selection enabling typically needs to happen in two instances:
 * <ol>
 * <li> Multiple table aliases are selected as multiple SelectValues. This typically happens when the output of a query is a single tuple with
 * multiple case classes in it (as is the case with the example above). Or a true nested entity (as these are supported in Spark).
 * The <code>runSuperAstParser</code> method covers this case.
 * <li> Multiple table aliases are inside of a single case-class SelectValue. This typically happens when a Ad-Hoc case class is used.
 * The <code>runCaseClassWithMultipleSelect</code> method covers this case.
 * </ol>
 *
 */
object SparkDialectRecursor {
  def runSuperAstParser(sparkIdiom: SparkIdiom, q: SqlQuery)(implicit strategy: NamingStrategy) = {
    import sparkIdiom._
    implicit val stableTokenizer = sparkIdiom.astTokenizer(new Tokenizer[Ast] {
      override def token(v: Ast): Token = astTokenizer(this, strategy).token(v)
    }, strategy)

    parentTokenizer.token(AliasNestedQueryColumns(q))
  }

  def runCaseClassWithMultipleSelect(values: List[(String, Ast)], sparkIdiom: SparkIdiom, prevContextMultipleSelect: Boolean)(implicit strategy: NamingStrategy) = {
    import sparkIdiom._
    implicit val stableTokenizer = sparkIdiom.astTokenizer(new Tokenizer[Ast] {
      override def token(v: Ast): Token = astTokenizer(this, strategy).token(v)
    }, strategy)

    if (prevContextMultipleSelect) // && values.length > 1) // comes from sparkIdiom
      stmt"(${values.map({ case (prop, value) => stmt"${TokenImplicit(value)(astTokenizer).token} AS ${prop.token}".token }).token})"
    else
      stmt"${values.map({ case (prop, value) => stmt"${TokenImplicit(value)(astTokenizer).token} AS ${prop.token}".token }).token}"
  }
}

trait SparkIdiom extends SqlIdiom with CannotReturn { self =>

  def parentTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy) = super.sqlQueryTokenizer

  def multipleSelect = false

  def liftingPlaceholder(index: Int): String = "?"

  override def prepareForProbing(string: String) = string

  override def translate(ast: Ast)(implicit naming: NamingStrategy) = {
    val normalizedAst = EscapeQuestionMarks(SqlNormalize(ast))

    implicit val tokernizer = defaultTokenizer

    val token =
      normalizedAst match {
        case q: Query =>
          val sql = SqlQuery(q)
          trace("sql")(sql)
          sql.token
        case other =>
          other.token
      }

    (normalizedAst, stmt"$token")
  }

  override def concatFunction = "explode"

  override implicit def identTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ident] = Tokenizer[Ident] {
    case Ident(name) =>
      if (multipleSelect)
        stmt"struct(${name.token}.*)"
      else
        stmt"${name.token}._1"
  }

  override implicit def sqlQueryTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SqlQuery] =
    Tokenizer[SqlQuery] {
      case q => {
        val nextMultipleSelect = q match {
          case f: FlattenSqlQuery => f.select.length > 1
          case _                  => false
        }
        val nextTokenizer = new SparkIdiom {
          override def multipleSelect: Boolean = nextMultipleSelect
        }
        SparkDialectRecursor.runSuperAstParser(nextTokenizer, q)
      }
    }

  override implicit def selectValueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[SelectValue] = Tokenizer[SelectValue] {
    case SelectValue(Ident(name), _, _) =>
      if (multipleSelect)
        stmt"struct(${strategy.default(name).token}.*)"
      else
        stmt"${strategy.default(name).token}.*"
    case other =>
      super.selectValueTokenizer.token(other)
  }

  override implicit def propertyTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Property] = {
    def path(ast: Ast): Token =
      ast match {
        case Ident(name) => name.token
        case Property.Opinionated(a, b, renameable, _) =>
          stmt"${path(a)}.${renameable.fixedOr(b.token)(strategy.column(b).token)}"
        case other =>
          other.token
      }
    Tokenizer[Property] {
      case p => path(p).token
    }
  }

  override implicit def operationTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Operation] = Tokenizer[Operation] {
    case BinaryOperation(a, StringOperator.`+`, b) => stmt"concat(${a.token}, ${b.token})"
    case op                                        => super.operationTokenizer.token(op)
  }

  override implicit def valueTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Value] = Tokenizer[Value] {
    case Constant(v: String) => stmt"'${v.replaceAll("""[\\']""", """\\$0""").token}'"
    case Tuple(values)       => stmt"(${values.token})"
    case CaseClass(values) => {
      val nextTokenizer = new SparkIdiom {
        override def multipleSelect: Boolean = values.length > 1
      }
      val keyValues = values.map { case (k, v) => (k, v) }
      SparkDialectRecursor.runCaseClassWithMultipleSelect(keyValues, nextTokenizer, self.multipleSelect)
    }
    case other => super.valueTokenizer.token(other)
  }

  override protected def tokenizeGroupBy(values: Ast)(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Token =
    values match {
      case Tuple(items) => items.mkStmt()
      case values       => values.token
    }
}

object SparkDialect extends SparkDialect

