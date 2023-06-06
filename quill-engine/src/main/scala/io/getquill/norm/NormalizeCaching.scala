package io.getquill.norm

import io.getquill.ast.Ast
import java.util.concurrent.ConcurrentHashMap

object NormalizeCaching {
  private val cache = new ConcurrentHashMap[Ast, Ast]

  def apply(f: Ast => Ast): Ast => Ast = { ori =>
    val (stabilized, state) = StabilizeLifts.stabilize(ori)
    val cachedR             = cache.get(stabilized)
    val normalized = if (cachedR != null) {
      cachedR
    } else {
      val r = f(stabilized)
      cache.put(stabilized, r)
      r
    }
    StabilizeLifts.revert(normalized, state)
  }

}
