package io.getquill

import scala.util.Success
import scala.util.Try
import org.apache.spark.sql.{ Column, Dataset, Row, SQLContext, Encoder => SparkEncoder }
import org.apache.spark.sql.functions.{ udf, struct, col }
import io.getquill.context.Context
import io.getquill.context.spark.Encoders
import io.getquill.context.spark.Decoders
import io.getquill.context.spark.SparkDialect
import io.getquill.context.spark.Binding
import io.getquill.context.spark.DatasetBinding
import io.getquill.context.spark.ValueBinding
import org.apache.spark.sql.types.{ StructField, StructType }
import io.getquill.context.spark.norm.QuestionMarkEscaper._

object QuillSparkContext extends QuillSparkContext

trait QuillSparkContext
  extends Context[SparkDialect, Literal]
  with Encoders
  with Decoders {

  type Result[T] = Dataset[T]
  type RunQuerySingleResult[T] = T
  type RunQueryResult[T] = T

  def close() = {}

  def probe(statement: String): Try[_] = Success(Unit)

  val idiom = SparkDialect
  val naming = Literal

  private implicit def datasetEncoder[T] =
    (idx: Int, ds: Dataset[T], row: List[Binding]) =>
      row :+ DatasetBinding(ds)

  def liftQuery[T](ds: Dataset[T]) =
    quote {
      infix"SELECT * FROM (${lift(ds)})".as[Query[T]]
    }

  // Helper class for the perculateNullArrays method
  case class StructElement(column: Column, structField: StructField) {
    def children: Array[StructElement] = structField.dataType match {
      case StructType(fields) => fields.map(f => StructElement(column.getField(f.name), f))
      case _                  => Array()
    }
  }

  /**
   * As a result of converting product objects from <code>value.*</code> selects in <code>ExpandEntityIds</code>,
   * when expressing empty options (e.g. as a result of left joins), we will get a array of null values instead of
   * a single null value like the Spark decode expects. See the issue #123 for more details.
   *
   * In order to fix this, we have to convert the array of nulls resulting from the selection of the
   * <code>o.bar</code>, and <code>o.baz</code> fields. We do this in two steps.
   * <ol>
   *   <li> We recursively traverse through <code>o</code>'s selected fields to see if they in turn have sub fields inside.
   *   For example, the selection could be <code>(o.bar, (o.waz, o.kaz) oo)</code>
   *   </li>
   *   <li> Then at every level of the recursion, we introduce a UDF that will nullify the entire row if all
   *   of the elements of that row are null.
   *   </li>
   * </ol>
   *
   * As a result of these two steps, arrays whose values are all null should be recursively translated to single null objects.
   */
  private def perculateNullArrays[T: SparkEncoder](ds: Dataset[T]) = {
    def nullifyNullArray(schema: StructType) = udf(
      (row: Row) => if ((0 until row.length).forall(row.isNullAt)) null else row,
      schema
    )

    def perculateNullArraysRecursive(node: StructElement): Column =
      node.structField.dataType match {
        case st: StructType =>
          // Recursively convert all parent array columns to single null values if all their children are null
          val preculatedColumn = struct(node.children.map(perculateNullArraysRecursive(_)): _*)
          // Then express that column back out the schema
          nullifyNullArray(st)(preculatedColumn).as(node.structField.name)
        case _ =>
          node.column.as(node.structField.name)
      }

    ds.select(
      ds.schema.fields.map(f => perculateNullArraysRecursive(StructElement(col(f.name), f))): _*
    ).as[T]
  }

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit enc: SparkEncoder[T], spark: SQLContext) =
    perculateNullArrays(spark.sql(prepareString(string, prepare)).as[T])

  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit enc: SparkEncoder[T], spark: SQLContext) =
    perculateNullArrays(spark.sql(prepareString(string, prepare)).as[T])

  private def prepareString(string: String, prepare: Prepare)(implicit spark: SQLContext) = {
    var dsId = 0
    val withSubstitutions =
      prepare(Nil)._2.foldLeft(string) {
        case (string, DatasetBinding(ds)) =>
          dsId += 1
          val name = s"ds$dsId"
          ds.createOrReplaceTempView(name)
          pluginValueSafe(string, name)
        case (string, ValueBinding(value)) =>
          pluginValueSafe(string, value)
      }
    unescape(withSubstitutions)
  }
}
