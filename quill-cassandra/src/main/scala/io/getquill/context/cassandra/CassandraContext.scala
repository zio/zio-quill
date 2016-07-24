package io.getquill.context.cassandra

import scala.language.higherKinds
import scala.language.experimental.macros
import scala.util.Try
import io.getquill.NamingStrategy
import io.getquill.context.Context

trait CassandraContext[N <: NamingStrategy, R, S]
  extends Context[R, S]
  with Ops {

  def probe(cql: String): Try[Unit]

  protected type QueryResult[T]
  protected type SingleQueryResult[T]
  protected type ActionResult[T]
  protected type BatchedActionResult[T]
  protected type Params[T]

  def run[T](
    quoted: Quoted[Query[T]]
  ): QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => Query[T]]
  ): P1 => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => Query[T]]
  ): (P1, P2) => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => Query[T]]
  ): (P1, P2, P3) => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => Query[T]]
  ): (P1, P2, P3, P4) => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => Query[T]]
  ): (P1, P2, P3, P4, P5) => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => Query[T]]
  ): (P1, P2, P3, P4, P5, P6) => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => Query[T]]
  ): (P1, P2, P3, P4, P5, P6, P7) => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => Query[T]]
  ): (P1, P2, P3, P4, P5, P6, P7, P8) => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => Query[T]]
  ): (P1, P2, P3, P4, P5, P6, P7, P8, P9) => QueryResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => Query[T]]
  ): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => QueryResult[T] = macro CassandraContextMacro.run[R, S]

  def run[T, O](
    quoted: Quoted[Action[T, O]]
  ): ActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, T, O](
    quoted: Quoted[P1 => Action[T, O]]
  ): Params[P1] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, T, O](
    quoted: Quoted[(P1, P2) => Action[T, O]]
  ): Params[(P1, P2)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, T, O](
    quoted: Quoted[(P1, P2, P3) => Action[T, O]]
  ): Params[(P1, P2, P3)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, T, O](
    quoted: Quoted[(P1, P2, P3, P4) => Action[T, O]]
  ): Params[(P1, P2, P3, P4)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T, O](
    quoted: Quoted[(P1, P2, P3, P4, P5) => Action[T, O]]
  ): Params[(P1, P2, P3, P4, P5)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T, O](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => Action[T, O]]
  ): Params[(P1, P2, P3, P4, P5, P6)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T, O](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => Action[T, O]]
  ): Params[(P1, P2, P3, P4, P5, P6, P7)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T, O](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => Action[T, O]]
  ): Params[(P1, P2, P3, P4, P5, P6, P7, P8)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T, O](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => Action[T, O]]
  ): Params[(P1, P2, P3, P4, P5, P6, P7, P8, P9)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T, O](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => Action[T, O]]
  ): Params[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10)] => BatchedActionResult[T] = macro CassandraContextMacro.run[R, S]

  def run[T](
    quoted: Quoted[T]
  ): SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => T]
  ): P1 => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => T]
  ): (P1, P2) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => T]
  ): (P1, P2, P3) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => T]
  ): (P1, P2, P3, P4) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => T]
  ): (P1, P2, P3, P4, P5) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => T]
  ): (P1, P2, P3, P4, P5, P6) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => T]
  ): (P1, P2, P3, P4, P5, P6, P7) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => T]
  ): (P1, P2, P3, P4, P5, P6, P7, P8) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => T]
  ): (P1, P2, P3, P4, P5, P6, P7, P8, P9) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => T]
  ): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => SingleQueryResult[T] = macro CassandraContextMacro.runSingle[R, S]
}
