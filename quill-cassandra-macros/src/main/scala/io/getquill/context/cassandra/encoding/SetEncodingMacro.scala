package io.getquill.context.cassandra.encoding

import scala.reflect.macros.blackbox.{ Context => MacroContext }

class SetEncodingMacro(val c: MacroContext) extends CollectionsEncodingMacro {
  import c.universe._

  def setEncoder[T](implicit tag: WeakTypeTag[T]): Tree = {
    val mapper = c.typecheck(encoderTypeMapper(tag.tpe))
    val casTpe = function2Types(mapper)._2
    c.typecheck {
      q"""
       val setMapper: (Set[$tag] => java.util.Set[$casTpe]) = set => collection.JavaConverters
         .setAsJavaSetConverter(set.map((x: $tag) => $mapper(x))).asJava

       encoder[Set[$tag]]((row: ${c.prefix}.PrepareRow) => (i: ${c.prefix}.Index, set: Set[$tag]) =>
         row.setSet(i, setMapper(set)))
      """
    }
  }

  def setDecoder[T](implicit tag: WeakTypeTag[T]): Tree = {
    val mapper = c.typecheck(decoderTypeMapper(tag.tpe))
    val casTpe = function2Types(mapper)._1
    c.typecheck {
      q"""
        val setMapper: (java.util.Set[$casTpe] => Set[$tag]) = set =>
          collection.JavaConverters.asScalaSetConverter(set).asScala.map((x: $casTpe) => $mapper(x)).toSet

        decoder((row: ${c.prefix}.ResultRow) => (i: ${c.prefix}.Index) =>
          setMapper(row.getSet[$casTpe](i, classOf[$casTpe])))
      """
    }
  }
}
