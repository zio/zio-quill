package io.getquill.norm

import io.getquill._
import io.getquill.ast._

class AttachToEntitySpec extends Spec {

  val attachToEntity = AttachToEntity(SortBy(_, _, Constant(1)))

  "attaches clause to the root of the query (entity)" - {
    "query is the entity" in {
      val n = quote {
        qr1.sortBy(x => 1)
      }
      attachToEntity(qr1.ast) mustEqual n.ast
    }
    "query is a composition" - {
      "map" in {
        val q = quote {
          qr1.filter(t => t.i == 1).map(t => t.s)
        }
        val n = quote {
          qr1.sortBy(t => 1).filter(t => t.i == 1).map(t => t.s)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "flatMap" in {
        val q = quote {
          qr1.filter(t => t.i == 1).flatMap(t => qr2)
        }
        val n = quote {
          qr1.sortBy(t => 1).filter(t => t.i == 1).flatMap(t => qr2)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "filter" in {
        val q = quote {
          qr1.filter(t => t.i == 1).filter(t => t.s == "s1")
        }
        val n = quote {
          qr1.sortBy(t => 1).filter(t => t.i == 1).filter(t => t.s == "s1")
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
      "sortBy" in {
        val q = quote {
          qr1.filter(t => t.i == 1).sortBy(t => t.s)
        }
        val n = quote {
          qr1.sortBy(t => 1).filter(t => t.i == 1).sortBy(t => t.s)
        }
        attachToEntity(q.ast) mustEqual n.ast
      }
    }
  }
}
