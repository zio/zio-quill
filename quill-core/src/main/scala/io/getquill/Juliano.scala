package io.getquill

import io.getquill.dsl.DynamicQueryDsl

object Juliano {
  val ctx = new TestMirrorContextTemplate(MirrorIdiom, Literal) with DynamicQueryDsl
  case class Oba(id: Long)

  def main(args: Array[String]): Unit = {
    import ctx._

    //        val q = query[Oba].filter(o => o.id > 1)

    println(ctx.run(query[Oba].filter(o => o.id > 2)).string)
  }
}

class TestMirrorContextTemplate[Dialect <: MirrorIdiomBase, Naming <: NamingStrategy](dialect: Dialect, naming: Naming)
  extends MirrorContext[Dialect, Naming](dialect, naming) {

  def withDialect[I <: MirrorIdiomBase](dialect: I)(f: TestMirrorContextTemplate[I, Naming] => Any): Unit = {
    val ctx = new TestMirrorContextTemplate[I, Naming](dialect, naming)
    f(ctx)
    ctx.close
  }
}
