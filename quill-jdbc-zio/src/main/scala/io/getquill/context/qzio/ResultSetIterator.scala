package io.getquill.context.qzio

import java.sql.{ Connection, ResultSet }

/**
 * In order to allow a ResultSet to be consumed by an Observable, a ResultSet iterator must be created.
 * Since Quill provides a extractor for an individual ResultSet row, a single row can easily be cached
 * in memory. This allows for a straightforward implementation of a hasNext method.
 */
class ResultSetIterator[T](rs: ResultSet, conn: Connection, extractor: (ResultSet, Connection) => T) extends BufferedIterator[T] {

  private[this] var state = 0 // 0: no data, 1: cached, 2: finished
  private[this] var cached: T = null.asInstanceOf[T]

  protected[this] final def finished(): T = {
    state = 2
    null.asInstanceOf[T]
  }

  /** Return a new value or call finished() */
  protected def fetchNext(): T =
    if (rs.next()) extractor(rs, conn)
    else finished()

  def head: T = {
    prefetchIfNeeded()
    if (state == 1) cached
    else throw new NoSuchElementException("head on empty iterator")
  }

  private def prefetchIfNeeded(): Unit = {
    if (state == 0) {
      cached = fetchNext()
      if (state == 0) state = 1
    }
  }

  def hasNext: Boolean = {
    prefetchIfNeeded()
    state == 1
  }

  def next(): T = {
    prefetchIfNeeded()
    if (state == 1) {
      state = 0
      cached
    } else throw new NoSuchElementException("next on empty iterator");
  }
}
