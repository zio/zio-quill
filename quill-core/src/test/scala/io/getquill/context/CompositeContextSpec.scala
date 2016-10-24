package io.getquill.context

import io.getquill.Spec
import io.getquill._
import io.getquill.idiom.Idiom

class CompositeContextSpec extends Spec {

  val c = CompositeContext[MirrorContext[MirrorIdiom, NamingStrategy]](
      CompositeContext.when(true)(new MirrorContext(MirrorIdiom, SnakeCase)), 
      CompositeContext.when(false)(new MirrorContext(MirrorIdiom, CamelCase)))

  import c._

  case class Person(personName: String)
  println(c.run(query[Person]).string)

}