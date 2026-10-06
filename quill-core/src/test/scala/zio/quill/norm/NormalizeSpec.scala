package zio.quill.norm

import zio.quill.base.Spec
import zio.quill.MirrorContexts.testContext.implicitOrd
import zio.quill.MirrorContexts.testContext.qr1
import zio.quill.MirrorContexts.testContext.qr2
import zio.quill.MirrorContexts.testContext.quote
import zio.quill.MirrorContexts.testContext.unquote

class NormalizeSpec extends Spec {

  val normalize = new Normalize(TranspileConfig.Empty)

  "normalizes random-generated queries" - {
    val gen = new QueryGenerator(1)
    for (i <- (3 to 15)) {
      for (j <- (0 until 30)) {
        val query = gen(i)
        s"$i levels ($j) - $query" in {
          // println("=================== Normalizing Query ==================\n" + query + "\n" + "=== Full ===" + "\n" + Messages.qprint(query).render)
          normalize(query)
          ()
        }
      }
    }
  }

  "doesn't apply the avoid capture normalization to branches in isolation" in {
    val q = quote {
      qr1.sortBy(t => t.i).flatMap(f => qr2.map(t => 1))
    }
    val n = quote {
      qr1.sortBy(t => t.i).flatMap(t => qr2.map(t1 => 1))
    }
    normalize(q.ast) mustEqual n.ast
  }
}
