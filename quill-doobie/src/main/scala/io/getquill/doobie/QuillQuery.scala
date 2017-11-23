package io.getquill.doobie

import java.sql.PreparedStatement
import java.sql.ResultSet

import _root_.doobie._
import _root_.doobie.implicits._

/**
 * Analog to `Query0` for queries defined via Quill, which provides query checking and type mapping
 * via its own mechanisms. doobie provides effect/resource handling.
 */
final class QuillQuery0[B](val sql: String, bind: FPS.PreparedStatementIO[Unit] => FPS.PreparedStatementIO[Unit], extractor: ResultSet => B) {

//  /** `Process` with effect type `ConnectionIO` yielding  elements of type `B`. */
//  def process: Process[ConnectionIO, B] = {
//    
//    val preparedStatement: Process[ConnectionIO, PreparedStatement] = 
//      resource(
//        FC.prepareStatement(sql))(ps =>
//        FC.liftPreparedStatement(ps, FPS.close))(ps =>
//        Option(ps).point[ConnectionIO]).take(1) // note
//  
//    def results(ps: PreparedStatement): Process[ConnectionIO, B] =
//      resource(
//        FC.liftPreparedStatement(ps, FPS.executeQuery))(rs =>
//        FC.liftResultSet(rs, FRS.close))(rs =>
//        FC.liftResultSet(rs, FRS.raw { rs =>
//          if (rs.next) Some(extractor(rs)) else None
//        }))
//
//    for {
//      ps <- preparedStatement
//      _  <- Process.eval(FC.liftPreparedStatement(ps, FPS.raw(bind).void))
//      a  <- results(ps)
//    } yield a
//
//  }
//
//  /**
//   * Construct a program that prepares and executes the query, folding the results with arbitrary 
//   * unsafe function `f`. This method is private to the implementation.
//   */
//  private def liftFoldRaw[A](f: ResultSet => A): ConnectionIO[A] =
//    HC.prepareStatement(sql)(FPS.raw(bind) *> HPS.executeQuery(FRS.raw(f)))
//
//  /**
//   * Program in `ConnectionIO` yielding an `F[B]` accumulated via the provided `CanBuildFrom`. This
//   * is the fastest way to accumulate a collection.
//   */
//  def to[F[_]](implicit cbf: CanBuildFrom[Nothing, B, F[B]]): ConnectionIO[F[B]] =
//    liftFoldRaw { rs =>
//      val acc = cbf()
//      while (rs.next)
//        acc += extractor(rs)
//      acc.result()
//    }
//
//  /**
//   * Program in `ConnectionIO` yielding an `F[B]` accumulated via `MonadPlus` append. This method 
//   * is more general but less efficient than `to`.
//   */
//  def accumulate[F[_]](implicit ev: MonadPlus[F]): ConnectionIO[F[B]] =
//    liftFoldRaw { rs =>
//      var acc = ev.empty[B]
//      while (rs.next) 
//        acc = ev.plus(acc, ev.point(extractor(rs)))
//      acc
//    }
//
//  /**
//   * Program in `ConnectionIO` yielding a unique `B` and raising an exception if the resultset does
//   * not have exactly one row. See also `option`.
//   */
//  def unique: ConnectionIO[B] =
//    liftFoldRaw { rs =>
//      if (rs.next) {
//        val r = extractor(rs)
//        if (rs.next) throw UnexpectedContinuation 
//        else r
//      } else throw UnexpectedEnd
//    }
//
//  /**
//   * Program in `ConnectionIO` yielding an optional `B` and raising an exception if the resultset
//   * has more than one row. See also `unique`.
//   */    
//  def option: ConnectionIO[Option[B]] =
//    liftFoldRaw { rs =>
//      if (rs.next) {
//        val r = extractor(rs)
//        if (rs.next) throw UnexpectedContinuation 
//        else Some(r)
//      } else None
//    }
//
//  /** Covariant map. */
//  def map[C](f: B => C): QuillQuery0[C] =
//    new QuillQuery0(sql, bind, rs => f(extractor(rs)))
//
//  /** Convenience method; equivalent to `process.sink(f)` */
//  def sink(f: B => ConnectionIO[Unit]): ConnectionIO[Unit] =
//    process.sink(f)
//
//  /** Convenience method; equivalent to `to[List]` */
//  def list: ConnectionIO[List[B]] =
//    to[List]
//
//  /** Convenience method; equivalent to `to[Vector]` */
//  def vector: ConnectionIO[Vector[B]] =
//    to[Vector]

}

object QuillQuery0 {

//  // QuillQuery0 is a covariant functor
//  implicit val QuillQuery0Functor: Functor[QuillQuery0] =
//    new Functor[QuillQuery0] {
//      def map[A, B](fa: QuillQuery0[A])(f: A => B): QuillQuery0[B] =
//        fa.map(f)
//    }

}
