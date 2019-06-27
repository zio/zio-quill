package io.getquill

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.FieldValueList
import io.getquill.context.Context
import io.getquill.context.bigquery.Encoders
import io.getquill.context.bigquery.Decoders
import io.getquill.util.ContextLogger
import scala.collection.JavaConverters._

class BigQueryContext[N <: NamingStrategy](val naming: N, bigquery: BigQuery)
  extends Context[PostgresDialect, N] // SQL 2011?
  with Encoders
  with Decoders {
  private[getquill] val logger = ContextLogger(classOf[BigQueryContext[_]])

  override type Result[T] = T
  override type RunQueryResult[T] = Iterable[T]
  override type RunQuerySingleResult[T] = T

  override type PrepareRow = QueryJobConfiguration.Builder
  override type ResultRow = FieldValueList

  // Members declared in java.io.Closeable
  def close() = {}

  // Members declared in io.getquill.context.Context
  def idiom = PostgresDialect

  def probe(statement: String): scala.util.Try[_] = ???

  def executeQuery[T](
    sql:       String,
    prepare:   Prepare      = identityPrepare,
    extractor: Extractor[T] = identityExtractor
  ): Result[Iterable[T]] = {
    val (params, builder) = prepare(QueryJobConfiguration.newBuilder(sql))
    logger.logQuery(sql, params)
    val tableResult = bigquery.query(builder.build())
    tableResult.iterateAll().asScala.map(extractor)
  }
}