package zio.quill.norm

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import zio.quill.ast.Ast
import zio.quill.util.Messages

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
