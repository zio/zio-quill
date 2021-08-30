package io.getquill.context.qzio

import zio.{ IO, ZIO }

/**
 * Use to provide `run(myQuery)` calls with a context implicitly saving the need to provide things multiple times.
 * For example in JDBC:
 * {{{
 *   case class MyQueryService(ds: DataSource with Closeable) {
 *     import Ctx._
 *     implicit val env = Implicit(Has(ds))
 *
 *     val joes = Ctx.run(query[Person].filter(p => p.name == "Joe")).onDataSource.implicitly
 *     val jills = Ctx.run(query[Person].filter(p => p.name == "Jill")).onDataSource.implicitly
 *     val alexes = Ctx.run(query[Person].filter(p => p.name == "Alex")).onDataSource.implicitly
 *   }
 * }}}
 * Normally you would have to do a separate `provide` for each clause:
 * {{{
 *   val joes = Ctx.run(query[Person].filter(p => p.name == "Joe")).onDataSource.provide(Has(ds))
 *   val jills = Ctx.run(query[Person].filter(p => p.name == "Jill")).onDataSource.provide(Has(ds))
 *   val alexes = Ctx.run(query[Person].filter(p => p.name == "Alex")).onDataSource.provide(Has(ds))
 * }}}
 *
 * For other contexts where the environment returned from `run(myQuery)` just the session itself,
 * usage is even simpler. For instance, in quill-zio-cassandra, you only need to specify `implicitly`.
 *
 * {{{
 *   case class MyQueryService(cs: CassandraZioSession) {
 *     import Ctx._
 *     implicit val env = Implicit(Has(cs))
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
    def implicitly(implicit r: Implicit[R]): IO[E, A] = self.provide(r.env)
  }
}
