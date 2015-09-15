package io.getquill.source

import io.getquill.quotation.Quoted
import io.getquill._
import io.getquill.ast._
import io.getquill.source.mirror.mirrorSource
import io.getquill.source.mirror.Row

class SourceMacroSpec extends Spec {

  "runs actions" - {
    "non-parametrized" - {
      "normal" in {
        val q = quote {
          qr1.delete
        }
        mirrorSource.run(q).ast mustEqual q.ast
      }
      "infix" in {
        val q = quote {
          infix"STRING".as[Actionable[TestEntity]]
        }
        mirrorSource.run(q).ast mustEqual Infix(List("STRING"), List())
      }
    }
    "parametrized" - {
      "normal" in {
        val q = quote {
          (a: String) => qr1.filter(t => t.s == a)
        }
        val r = mirrorSource.run(q).using("a")
        r.ast.toString mustEqual "queryable[TestEntity].filter(t => t.s == ?).map(t => (t.s, t.i, t.l))"
        r.binds mustEqual Row("a")
      }
      "infix" in {
        val q = quote {
          (a: String) => infix"t = $a".as[Actionable[TestEntity]]
        }
        val r = mirrorSource.run(q).using(List("a"))
        r.ast.toString mustEqual """infix"t = $?""""
        r.bindList mustEqual List(Row("a"))
      }
    }
  }

  "fails if the quotation is not runnable" in {
    val q = quote {
      (s: String) => s
    }
    "mirrorSource.run(q)" mustNot compile
  }

  "fails if unquotation fails" in {
    val q: Quoted[Int] = null
    "mirrorSource.run(q)" mustNot compile
  }
}
