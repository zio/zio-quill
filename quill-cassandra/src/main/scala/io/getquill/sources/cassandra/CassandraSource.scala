package io.getquill.sources.cassandra

import scala.language.higherKinds
import scala.language.experimental.macros
import scala.util.Try
import com.datastax.driver.core.Session
import io.getquill._
import io.getquill.quotation.Quoted
import io.getquill.naming.NamingStrategy
import io.getquill.sources.Source
import com.datastax.driver.core.ConsistencyLevel

trait CassandraSource[N <: NamingStrategy, R, S]
    extends Source[R, S] {

  def probe(cql: String): Try[Unit]

  type QueryResult[T]
  type ActionResult[T]
  type BatchedActionResult[T]
  type Params[T]

  def run[T](
    quoted: Quoted[Query[T]]): QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => Query[T]]): P1 => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => Query[T]]): (P1, P2) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => Query[T]]): (P1, P2, P3) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => Query[T]]): (P1, P2, P3, P4) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => Query[T]]): (P1, P2, P3, P4, P5) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => Query[T]]): (P1, P2, P3, P4, P5, P6) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]

  def run[T](
    quoted: Quoted[Action[T]]): ActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => Action[T]]): Params[P1] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => Action[T]]): Params[(P1, P2)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => Action[T]]): Params[(P1, P2, P3)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => Action[T]]): Params[(P1, P2, P3, P4)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => Action[T]]): Params[(P1, P2, P3, P4, P5)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => Action[T]]): Params[(P1, P2, P3, P4, P5, P6)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => Action[T]]): Params[(P1, P2, P3, P4, P5, P6, P7)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => Action[T]]): Params[(P1, P2, P3, P4, P5, P6, P7, P8)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => Action[T]]): Params[(P1, P2, P3, P4, P5, P6, P7, P8, P9)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => Action[T]]): Params[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]

  def run[T](
    quoted: Quoted[T]): QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => T]): P1 => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => T]): (P1, P2) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => T]): (P1, P2, P3) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => T]): (P1, P2, P3, P4) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => T]): (P1, P2, P3, P4, P5) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => T]): (P1, P2, P3, P4, P5, P6) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => T]): (P1, P2, P3, P4, P5, P6, P7) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => T]): (P1, P2, P3, P4, P5, P6, P7, P8) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
}
