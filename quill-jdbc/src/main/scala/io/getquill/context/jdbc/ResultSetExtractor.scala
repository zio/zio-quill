package io.getquill.context.jdbc
import java.sql.ResultSet

import scala.annotation.tailrec

object ResultSetExtractor {

  private[getquill] final def apply[T](rs: ResultSet, extractor: ResultSet => T): List[T] =
    extractResult(rs, extractor, List())

  @tailrec
  private[getquill] final def extractResult[T](rs: ResultSet, extractor: ResultSet => T, acc: List[T]): List[T] =
    if (rs.next)
      extractResult(rs, extractor, extractor(rs) :: acc)
    else
      acc.reverse
}
