package io.getquill

import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.{ Entity, PropertyAlias }
import io.getquill.context.mirror.Row

class UnlimitedOptionalEmbeddedSpec extends Spec {
  val ctx = testContext

  import ctx._

  case class Emb3(value: String) extends Embedded
  case class Emb2(e1: Emb3, e2: Option[Emb3]) extends Embedded
  case class Emb1(e1: Emb2, e2: Option[Emb2]) extends Embedded
  case class OptEmd(e1: Emb1, e2: Option[Emb1])

  lazy val optEmdEnt = OptEmd(
    Emb1(
      Emb2(Emb3("111"), Some(Emb3("112"))), Some(Emb2(Emb3("121"), Some(Emb3("122"))))
    ),
    Some(Emb1(
      Emb2(Emb3("211"), Some(Emb3("212"))), Some(Emb2(Emb3("221"), Some(Emb3("222"))))
    ))
  )

  val qrOptEmd = quote {
    querySchema[OptEmd](
      "OptEmd",
      _.e1.e1.e1.value -> "value111",
      _.e1.e1.e2.map(_.value) -> "value112",
      _.e1.e2.map(_.e1.value) -> "value121",
      _.e1.e2.map(_.e2.map(_.value)) -> "value122",
      _.e2.map(_.e1.e1.value) -> "value211",
      _.e2.map(_.e1.e2.map(_.value)) -> "value212",
      _.e2.map(_.e2.map(_.e1.value)) -> "value221",
      _.e2.map(_.e2.map(_.e2.map(_.value))) -> "value222"
    )
  }

  "quotation aliases" in {
    quote(unquote(qrOptEmd)).ast mustEqual Entity.Opinionated("OptEmd", List(
      PropertyAlias(List("e1", "e1", "e1", "value"), "value111"),
      PropertyAlias(List("e1", "e1", "e2", "value"), "value112"),
      PropertyAlias(List("e1", "e2", "e1", "value"), "value121"),
      PropertyAlias(List("e1", "e2", "e2", "value"), "value122"),
      PropertyAlias(List("e2", "e1", "e1", "value"), "value211"),
      PropertyAlias(List("e2", "e1", "e2", "value"), "value212"),
      PropertyAlias(List("e2", "e2", "e1", "value"), "value221"),
      PropertyAlias(List("e2", "e2", "e2", "value"), "value222")
    ), Fixed)
  }

  "meta" - {
    "query" in {
      materializeQueryMeta[OptEmd].expand.toString mustEqual "(q) => q.map(x => (" +
        "x.e1.e1.e1.value, " +
        "x.e1.e1.e2.map((v) => v.value), " +
        "x.e1.e2.map((v) => v.e1.value), " +
        "x.e1.e2.map((v) => v.e2.map((v) => v.value)), " +
        "x.e2.map((v) => v.e1.e1.value), " +
        "x.e2.map((v) => v.e1.e2.map((v) => v.value)), " +
        "x.e2.map((v) => v.e2.map((v) => v.e1.value)), " +
        "x.e2.map((v) => v.e2.map((v) => v.e2.map((v) => v.value)))))"
    }
    "update" in {
      materializeUpdateMeta[OptEmd].expand.toString mustEqual "(q, value) => q.update(" +
        "v => v.e1.e1.e1.value -> value.e1.e1.e1.value, " +
        "v => v.e1.e1.e2.map((v) => v.value) -> value.e1.e1.e2.map((v) => v.value), " +
        "v => v.e1.e2.map((v) => v.e1.value) -> value.e1.e2.map((v) => v.e1.value), " +
        "v => v.e1.e2.map((v) => v.e2.map((v) => v.value)) -> value.e1.e2.map((v) => v.e2.map((v) => v.value)), " +
        "v => v.e2.map((v) => v.e1.e1.value) -> value.e2.map((v) => v.e1.e1.value), " +
        "v => v.e2.map((v) => v.e1.e2.map((v) => v.value)) -> value.e2.map((v) => v.e1.e2.map((v) => v.value)), " +
        "v => v.e2.map((v) => v.e2.map((v) => v.e1.value)) -> value.e2.map((v) => v.e2.map((v) => v.e1.value)), " +
        "v => v.e2.map((v) => v.e2.map((v) => v.e2.map((v) => v.value))) -> value.e2.map((v) => v.e2.map((v) => v.e2.map((v) => v.value))))"
    }
    "insert" in {
      materializeInsertMeta[OptEmd].expand.toString mustEqual "(q, value) => q.insert(" +
        "v => v.e1.e1.e1.value -> value.e1.e1.e1.value, " +
        "v => v.e1.e1.e2.map((v) => v.value) -> value.e1.e1.e2.map((v) => v.value), " +
        "v => v.e1.e2.map((v) => v.e1.value) -> value.e1.e2.map((v) => v.e1.value), " +
        "v => v.e1.e2.map((v) => v.e2.map((v) => v.value)) -> value.e1.e2.map((v) => v.e2.map((v) => v.value)), " +
        "v => v.e2.map((v) => v.e1.e1.value) -> value.e2.map((v) => v.e1.e1.value), " +
        "v => v.e2.map((v) => v.e1.e2.map((v) => v.value)) -> value.e2.map((v) => v.e1.e2.map((v) => v.value)), " +
        "v => v.e2.map((v) => v.e2.map((v) => v.e1.value)) -> value.e2.map((v) => v.e2.map((v) => v.e1.value)), " +
        "v => v.e2.map((v) => v.e2.map((v) => v.e2.map((v) => v.value))) -> value.e2.map((v) => v.e2.map((v) => v.e2.map((v) => v.value))))"
    }
  }

  "action" - {
    val resultString = """`querySchema`("OptEmd", _.e1.e1.e1.value -> "value111", _.e1.e1.e2.value -> "value112", """ +
      """_.e1.e2.e1.value -> "value121", _.e1.e2.e2.value -> "value122", _.e2.e1.e1.value -> "value211", """ +
      """_.e2.e1.e2.value -> "value212", _.e2.e2.e1.value -> "value221", _.e2.e2.e2.value -> "value222").insert(""" +
      "v => v.e1.e1.e1.value -> ?, " +
      "v => v.e1.e1.e2.map((v) => v.value) -> ?, " +
      "v => v.e1.e2.map((v) => v.e1.value) -> ?, " +
      "v => v.e1.e2.map((v) => v.e2.map((v) => v.value)) -> ?, " +
      "v => v.e2.map((v) => v.e1.e1.value) -> ?, " +
      "v => v.e2.map((v) => v.e1.e2.map((v) => v.value)) -> ?, " +
      "v => v.e2.map((v) => v.e2.map((v) => v.e1.value)) -> ?, " +
      "v => v.e2.map((v) => v.e2.map((v) => v.e2.map((v) => v.value))) -> ?)"
    val resultRow = Row("111", Some("112"), Some("121"), Some(Some("122")), Some("211"), Some(Some("212")), Some(Some("221")), Some(Some(Some("222"))))

    "non-batched" in {
      val r = testContext.run(qrOptEmd.insert(lift(optEmdEnt)))
      r.string mustEqual resultString
      r.prepareRow mustEqual resultRow
    }
    "batched" in {
      val r = testContext.run(liftQuery(List(optEmdEnt)).foreach(e => qrOptEmd.insert(e)))
      r.groups mustEqual List(resultString -> List(resultRow))
    }
  }
}
