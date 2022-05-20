package io.getquill.norm

import io.getquill.ast.Ast
import java.util.concurrent.ConcurrentHashMap

object NormalizeCaching {
  private val MAX_ENTRIES = 100
  private val cache = new ConcurrentHashMap[Ast, Ast]

  def apply(f: Ast => Ast): Ast => Ast = { ori =>
    val (stablized, state) = StablizeLifts.stablize(ori)
    val cachedR = cache.get(stablized)
    val normalized = if (cachedR != null) {
      cachedR
    } else {
      if(cache.size > MAX_ENTRIES) cache.clear()
      val r = f(stablized)
      cache.put(stablized, r)
      r
    }
    StablizeLifts.revert(normalized, state)
  }

}
