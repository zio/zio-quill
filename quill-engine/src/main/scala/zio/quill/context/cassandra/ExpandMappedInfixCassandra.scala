package zio.quill.context.cassandra

import zio.quill.ast.StatelessTransformer
import zio.quill.ast._

object ExpandMappedInfixCassandra extends StatelessTransformer {

  override def apply(q: Ast) =
    q match {
      case Map(q: Infix, x, p) if (x == p) =>
        q
      case q @ Map(Infix(parts, params, pure, tr, quat), x, p) =>
        params.zipWithIndex.collect { case (q: Query, i) =>
          (q, i)
        } match {
          case List((q, i)) =>
            Infix(parts, params.updated(i, Map(q, x, p)), pure, tr, quat)
          case other =>
            super.apply(q)
        }
      case other =>
        super.apply(q)
    }
}
