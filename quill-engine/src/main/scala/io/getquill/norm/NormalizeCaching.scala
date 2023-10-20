package io.getquill.norm

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import io.getquill.ast.Ast
import io.getquill.util.Messages

object NormalizeCaching {

  private val cache: Cache[Ast, Ast] = Caffeine
    .newBuilder()
    .maximumSize(Messages.cacheDynamicMaxSize)
    .recordStats()
    .build()

  def apply(f: Ast => Ast): Ast => Ast = { ori =>
    val (stabilized, state) = StabilizeLifts.stabilize(ori)
    val normalized          = cache.get(stabilized, ast => f(ast))
    StabilizeLifts.revert(normalized, state)
  }

}
