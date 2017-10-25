package io.getquill

import scala.util.Success
import scala.util.Try
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.{ Encoder => SparkEncoder }
import org.apache.spark.sql.SQLContext
import io.getquill.context.Context
import org.apache.spark.rdd.RDD
import language.implicitConversions
import io.getquill.context.spark.Encoders
import io.getquill.context.spark.Decoders
import io.getquill.context.spark.SparkDialect
import io.getquill.context.spark.Binding
import io.getquill.context.spark.DatasetBinding
import io.getquill.context.spark.ValueBinding
import scala.reflect.ClassTag

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

  case class ToQuery[T](ds: Dataset[T]) {
    def toQuery =
      quote {
        infix"${lift(ds)}".as[Query[T]]
      }
  }

  implicit def seqToQuery[T: SparkEncoder: ClassTag](t: Seq[T])(implicit spark: SQLContext) =
    rddToQuery(spark.sparkContext.parallelize(t))

  implicit def datasetToQuery[T](ds: Dataset[T]) = ToQuery(ds)

  implicit def rddToQuery[T: SparkEncoder](ds: RDD[T])(implicit spark: SQLContext) = {
    import spark.implicits._
    ToQuery(ds.toDS)
  }

  def executeQuery[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit enc: SparkEncoder[T], spark: SQLContext) =
    spark.sql(prepareString(string, prepare)).as[T]

  def executeQuerySingle[T](string: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor)(implicit enc: SparkEncoder[T], spark: SQLContext) =
    spark.sql(prepareString(string, prepare)).as[T]

  private def prepareString(string: String, prepare: Prepare)(implicit spark: SQLContext) = {
    var dsId = 0
    prepare(Nil)._2.foldLeft(string) {
      case (string, DatasetBinding(ds)) =>
        dsId += 1
        val name = s"ds$dsId"
        ds.createOrReplaceTempView(name)
        string.replaceFirst("\\?", name)
      case (string, ValueBinding(value)) =>
        string.replaceFirst("\\?", value)
    }
  }
}
