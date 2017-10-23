package io.getquill.context.sql

import io.getquill.Spec
import io.getquill.context.sql.encoding.ArrayEncoding

trait ArrayOpsSpec extends Spec {

  val ctx: SqlContext[_, _] with ArrayEncoding

  import ctx._

  case class ArrayOps(id: Int, numbers: Seq[Int])

  val entriesList = List(
    ArrayOps(1, List(1, 2, 3)),
    ArrayOps(2, List(1, 4, 5)),
    ArrayOps(3, List(1, 4, 6))
  )

  val entity = quote(query[ArrayOps])

  val insertEntries = quote {
    liftQuery(entriesList).foreach(e => entity.insert(e))
  }

  object `contains` {
    def idByContains(x: Int) = quote(entity.filter(_.numbers.contains(lift(x))).map(_.id))

    val `Ex 1 return all` = quote(idByContains(1))
    val `Ex 1 expected` = List(1, 2, 3)

    val `Ex 2 return 1` = quote(idByContains(3))
    val `Ex 2 expected` = List(1)

    val `Ex 3 return 2,3` = quote(idByContains(4))
    val `Ex 3 expected` = List(2, 3)

    val `Ex 4 return empty` = quote(idByContains(10))
    val `Ex 4 expected` = Nil
  }
}
