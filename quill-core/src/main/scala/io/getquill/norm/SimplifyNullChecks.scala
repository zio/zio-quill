package io.getquill.norm

import io.getquill.ast._
import io.getquill.ast.Implicits._

/**
 * Due to the introduction of null checks in `map`, `flatMap`, and `exists`, in
 * `FlattenOptionOperation` in order to resolve #1053, as well as to support non-ansi
 * compliant string concatenation as outlined in #1295, large conditional composites
 * became common. For example:
 * <code><pre>
 * case class Holder(value:Option[String])
 *
 * // The following statement
 * query[Holder].map(h => h.value.map(_ + "foo"))
 * // Will yield the following result
 * SELECT CASE WHEN h.value IS NOT NULL THEN h.value || 'foo' ELSE null END FROM Holder h
 * </pre></code>
 * Now, let's add a <code>getOrElse</code> statement to the clause that requires an additional
 * wrapped null check. We cannot rely on there being a <code>map</code> call beforehand
 * since we could be reading <code>value</code> as a nullable field directly from the database).
 * <code><pre>
 * // The following statement
 * query[Holder].map(h => h.value.map(_ + "foo").getOrElse("bar"))
 * // Yields the following result:
 * SELECT CASE WHEN
 * CASE WHEN h.value IS NOT NULL THEN h.value || 'foo' ELSE null END
 * IS NOT NULL THEN
 * CASE WHEN h.value IS NOT NULL THEN h.value || 'foo' ELSE null END
 * ELSE 'bar' END FROM Holder h
 * </pre></code>
 * This of course is highly redundant and can be reduced to simply:
 * <code><pre>
 * SELECT CASE WHEN h.value IS NOT NULL AND (h.value || 'foo') IS NOT NULL THEN h.value || 'foo' ELSE 'bar' END FROM Holder h
 * </pre></code>
 * This reduction is done by the "Center Rule." There are some other simplification
 * rules as well. Note how we are force to null-check both `h.value` as well as `(h.value || 'foo')` because
 * a user may use `Option[T].flatMap` and explicitly transform a particular value to `null`.
 */
object SimplifyNullChecks extends StatelessTransformer {

  override def apply(ast: Ast): Ast =
    ast match {

      // Center rule
      case IfExist(
        IfExistElseNull(condA, thenA),
        IfExistElseNull(condB, thenB),
        otherwise
        ) if (condA == condB && thenA == thenB) =>
        apply(If(Exist(condA) +&&+ Exist(thenA), thenA, otherwise))

      // Left hand rule
      case IfExist(IfExistElseNull(check, affirm), value, otherwise) =>
        apply(If(Exist(check) +&&+ Exist(affirm), value, otherwise))

      // Right hand rule
      case IfExistElseNull(cond, IfExistElseNull(innerCond, innerThen)) =>
        apply(If(Exist(cond) +&&+ Exist(innerCond), innerThen, NullValue))

      case other =>
        super.apply(other)
    }

}
