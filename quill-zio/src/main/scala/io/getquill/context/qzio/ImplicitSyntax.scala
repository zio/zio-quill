package io.getquill.context.qzio

import zio.{ IO, Tag, ZEnvironment, ZIO }

/**
 * Use to provide `run(myQuery)` calls with a context implicitly saving the need to provide things multiple times.
 * For example in JDBC:
 * {{{
 *   case class MyQueryService(ds: DataSource) {
 *     import Ctx._
 *     implicit val env = Implicit(Has(ds))
 *
 *     val joes = Ctx.run(query[Person].filter(p => p.name == "Joe")).implicitly
 *     val jills = Ctx.run(query[Person].filter(p => p.name == "Jill")).implicitly
 *     val alexes = Ctx.run(query[Person].filter(p => p.name == "Alex")).implicitly
 *   }
 * }}}
 * Normally you would have to do a separate `provide` for each clause:
 * {{{
 *   val joes = Ctx.run(query[Person].filter(p => p.name == "Joe")).provide(Has(ds))
 *   val jills = Ctx.run(query[Person].filter(p => p.name == "Jill")).provide(Has(ds))
 *   val alexes = Ctx.run(query[Person].filter(p => p.name == "Alex")).provide(Has(ds))
 * }}}
 *
 * For other contexts e.g. Cassandra the functionality is identical.
 *
 * {{{
 *   case class MyQueryService(cs: CassandraZioSession) {
 *     import Ctx._
 *     implicit val env = Implicit(cs)
 *
 *     def joes = Ctx.run { query[Person].filter(p => p.name == "Joe") }.implicitly
 *     def jills = Ctx.run { query[Person].filter(p => p.name == "Jill") }.implicitly
 *     def alexes = Ctx.run { query[Person].filter(p => p.name == "Alex") }.implicitly
 *   }
 * }}}
 *
 */
object ImplicitSyntax {
  /** A new type that indicates that the value `R` should be made available to the environment implicitly. */
  final case class Implicit[R](env: R)

  implicit final class ImplicitSyntaxOps[R, E, A](private val self: ZIO[R, E, A]) extends AnyVal {
    def implicitly(implicit r: Implicit[R], tag: Tag[R]): IO[E, A] = self.provideEnvironment(ZEnvironment(r.env))
  }
}
