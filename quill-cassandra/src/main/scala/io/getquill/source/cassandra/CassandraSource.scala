package io.getquill.source.cassandra

import scala.language.higherKinds
import scala.language.experimental.macros
import scala.util.Try

import com.datastax.driver.core.Session

import io.getquill._
import io.getquill.quotation.Quoted
import io.getquill.naming.NamingStrategy
import io.getquill.source.Source

trait CassandraSource[N <: NamingStrategy, R, S]
    extends Source[R, S] {

  def probe(cql: String): Try[Unit]

  type QueryResult[T]
  type ActionResult[T]
  type BatchedActionResult[T]

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
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) => Query[T]]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]

  def run[T](
    quoted: Quoted[Action[T]]): ActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, T](
    quoted: Quoted[P1 => Action[T]]): List[P1] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, T](
    quoted: Quoted[(P1, P2) => Action[T]]): List[(P1, P2)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, T](
    quoted: Quoted[(P1, P2, P3) => Action[T]]): List[(P1, P2, P3)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, T](
    quoted: Quoted[(P1, P2, P3, P4) => Action[T]]): List[(P1, P2, P3, P4)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, T](
    quoted: Quoted[(P1, P2, P3, P4, P5) => Action[T]]): List[(P1, P2, P3, P4, P5)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6) => Action[T]]): List[(P1, P2, P3, P4, P5, P6)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) => Action[T]]): List[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22)] => BatchedActionResult[T] = macro CassandraSourceMacro.run[R, S]

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
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
  def run[P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, T](
    quoted: Quoted[(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) => T]): (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) => QueryResult[T] = macro CassandraSourceMacro.run[R, S]
}
