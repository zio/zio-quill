package zio.quill.context.sql.idiom

import zio.quill.base.Spec
import zio.quill.context.sql.testContext._
import zio.quill.context.sql.{SqlQuery, SqlQueryApply}

import scala.util.Try
import zio.quill.context.sql.norm.SqlNormalize
import zio.quill.norm.TranspileConfig
import zio.quill.util.TraceConfig

class VerifySqlQuerySpec extends Spec {
  val SqlQuery = new SqlQueryApply(TraceConfig.Empty)

  "fails if the query can't be translated to applicative joins" - {
    "sortBy" in {
      val q = quote {
        qr1.flatMap(a => qr2.filter(b => b.s == a.s).sortBy(b => b.s).map(b => b.s))
      }
      VerifySqlQuery(SqlQuery(q.ast)).isDefined mustEqual true
    }

    "take" in {
      val q = quote {
        qr1.flatMap(a => qr2.filter(b => b.s == a.s).take(10).map(b => b.s))
      }
      VerifySqlQuery(SqlQuery(q.ast)).isDefined mustEqual true
    }

    "doesn't accept table reference" - {
      "with filter" in {
        val q = quote {
          qr1.leftJoin(qr2).on((a, b) => a.i == b.i).filter { case (a, b) =>
            b.isDefined
          }
        }

        an[IllegalArgumentException] should be thrownBy VerifySqlQuery(
          SqlQuery(SqlNormalize(q.ast, TranspileConfig.Empty))
        )
      }

      "with map" in {
        val q = quote {
          qr1
            .leftJoin(qr2)
            .on((a, b) => a.i == b.i)
            .map(pcTup => if (pcTup._2.isDefined) "bar" else "baz")
        }

        an[IllegalArgumentException] should be thrownBy VerifySqlQuery(
          SqlQuery(SqlNormalize(q.ast, TranspileConfig.Empty))
        )
      }
    }

    "invalid flatJoin on" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if a.i == b.i
          c <- qr1.leftJoin(_.i == a.i)
        } yield (a.i, b.i, c.map(_.i))
      }
      Try(VerifySqlQuery(SqlQuery(q.ast))).isFailure mustEqual true
    }

  }
}
