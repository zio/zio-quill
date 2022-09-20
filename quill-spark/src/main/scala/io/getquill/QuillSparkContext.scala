package io.getquill

import java.util.concurrent.atomic.AtomicInteger
import scala.util.Success
import scala.util.Try
import org.apache.spark.sql.{Column, Dataset, SQLContext, Encoder => SparkEncoder}
import org.apache.spark.sql.functions.{col, struct}
import io.getquill.context.{Context, ExecutionInfo}
import io.getquill.context.spark.Encoders
import io.getquill.context.spark.Decoders
import io.getquill.context.spark.SparkDialect
import io.getquill.context.spark.Binding
import io.getquill.context.spark.DatasetBinding
import io.getquill.context.spark.ValueBinding
import org.apache.spark.sql.types.{StructField, StructType}
import io.getquill.context.spark.norm.QuestionMarkEscaper._
import io.getquill.quat.QuatMaking
import org.apache.spark.sql.functions._

import scala.reflect.runtime.universe.TypeTag

object QuillSparkContext extends QuillSparkContext

trait QuillSparkContext extends Context[SparkDialect, Literal] with Encoders with Decoders {

  type Result[T]               = Dataset[T]
  type RunQuerySingleResult[T] = T
  type RunQueryResult[T]       = T
  type Session                 = Unit
  type Runner                  = Unit

  override type NullChecker = DummyNullChecker
  class DummyNullChecker extends BaseNullChecker {
    // The Quill Spark contexts uses Spark's internal decoders val dataFrame.as[MyRecord] so this is not necessary
    override def apply(index: Index, row: ResultRow): Boolean = false
  }
  implicit val nullChecker: NullChecker = new DummyNullChecker()

  implicit val ignoreDecoders: QuatMaking.IgnoreDecoders = QuatMaking.IgnoreDecoders

  private[getquill] val queryCounter = new AtomicInteger(0)

  def close() = {}

  def probe(statement: String): Try[_] = Success(())

  val idiom  = SparkDialect
  val naming = Literal

  private implicit def datasetEncoder[T] =
    (idx: Int, ds: Dataset[T], row: List[Binding], session: Session) => row :+ DatasetBinding(ds)

  def liftQuery[T](ds: Dataset[T]) =
    quote {
      sql"${lift(ds)}".pure.as[Query[T]]
    }

  // Helper class for the percolateNullArrays method
  case class StructElement(column: Column, structField: StructField) {
    def children: Array[StructElement] = structField.dataType match {
      case StructType(fields) => fields.map(f => StructElement(column.getField(f.name), f))
      case _                  => Array()
    }
  }

  /**
   * As a result of converting product objects from <code>value.*</code> selects
   * in the <code>SparkDialect</code> tokenizer, when expressing empty options
   * (e.g. as a result of left joins), we will get a array of null values
   * instead of a single null value like the Spark decode expects. See the issue
   * #123 for more details.
   *
   * In order to fix this, we have to convert the array of nulls resulting from
   * the selection of the <code>o.bar</code>, and <code>o.baz</code> fields. We
   * do this in two steps. <ol> <li> We recursively traverse through
   * <code>o</code>'s selected fields to see if they in turn have sub fields
   * inside. For example, the selection could be <code>(o.bar, (o.waz, o.kaz)
   * oo)</code> </li> <li> Then at every level of the recursion, we introduce a
   * UDF that will nullify the entire row if all of the elements of that row are
   * null. </li> </ol>
   *
   * As a result of these two steps, arrays whose values are all null should be
   * recursively translated to single null objects.
   */
  private def percolateNullArrays[T: SparkEncoder](ds: Dataset[T]) = {

    def percolateNullArraysRecursive(node: StructElement): Column =
      node.structField.dataType match {
        case st: StructType =>
          // Recursively convert all parent array columns to single null values if all their children are null
          val preculatedColumn = struct(node.children.map(percolateNullArraysRecursive(_)).toIndexedSeq: _*)
          // Then express that column back out the schema

          val mapped =
            (c: Column) => {
              when(
                st.fieldNames.map(c.getField(_)).foldLeft(lit(true)) { case (curr, col) => curr && col.isNull },
                lit(null: Column)
              ).otherwise(c)
            }

          mapped(preculatedColumn).as(node.structField.name)
        case _ =>
          node.column.as(node.structField.name)
      }

    ds.select(
      ds.schema.fields.map(f => percolateNullArraysRecursive(StructElement(col(f.name), f))).toIndexedSeq: _*
    ).as[T]
  }

  /**
   * As a result of expanding <code>value.*</code> selects in
   * <code>SparkDialect</code>, the aliases of the elements must align with the
   * outermost object Typically this will be a tuple but can be an ad-hoc case
   * class as well. For example say that the user makes the following query:
   * <br/><code>Query[Foo].join(Query[Bar]).on(_.id == _.fk)</code>
   *
   * The resulting query would be the following: <br/><code>select struct(f.*),
   * struct(b.*) from Foo f join Bar b on f.id = b.fk</code>
   *
   * However, since the outermost return type must be a tuple, the query must be
   * the following: <br/><code>select struct(f.*) _1, struct(b.*) _2 from Foo f
   * join Bar b on f.id = b.fk</code>
   *
   * These <code>_1</code> and <code>_2</code> aliases are added by this object.
   * In the edge case where they cannot be found (typically this happens when
   * single primitive values are selected), the outputs are assumed to be tuple
   * indexes i.e. _1, _2, etc...
   */
  private[getquill] object CaseAccessors {
    import scala.reflect.runtime.universe._

    def apply[T: TypeTag](schema: StructType): List[String] = {
      val out =
        typeOf[T].members.collect {
          case m: MethodSymbol if m.isCaseAccessor => m.name.toString
        }.toList.reverse

      if (out.size == schema.size) out
      else (1 to schema.size).map(i => s"_${i}").toList
    }
  }

  def executeQuery[T: TypeTag](
    string: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner)(implicit enc: SparkEncoder[T], spark: SQLContext) = {
    val ds = spark.sql(prepareString(string, prepare))
    percolateNullArrays(ds.toDF(CaseAccessors[T](ds.schema): _*).as[T])
  }

  def executeQuerySingle[T: TypeTag](
    string: String,
    prepare: Prepare = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  )(info: ExecutionInfo, dc: Runner)(implicit enc: SparkEncoder[T], spark: SQLContext) = {
    val ds = spark.sql(prepareString(string, prepare))
    percolateNullArrays(ds.toDF(CaseAccessors[T](ds.schema): _*).as[T])
  }

  private[getquill] def prepareString(string: String, prepare: Prepare)(implicit spark: SQLContext) = {
    var dsId = 0
    val withSubstitutions =
      prepare(Nil, ())._2.foldLeft(string) {
        case (string, DatasetBinding(ds)) =>
          dsId += 1
          val name = s"ds${queryCounter.incrementAndGet()}"
          ds.createOrReplaceTempView(name)
          pluginValueSafe(string, name)
        case (string, ValueBinding(value)) =>
          pluginValueSafe(string, value)
      }
    unescape(withSubstitutions)
  }
}
