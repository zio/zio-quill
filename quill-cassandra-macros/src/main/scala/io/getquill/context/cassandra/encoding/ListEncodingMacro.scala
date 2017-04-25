package io.getquill.context.cassandra.encoding

import scala.reflect.macros.blackbox.{ Context => MacroContext }

class ListEncodingMacro(val c: MacroContext) extends CollectionsEncodingMacro {
  import c.universe._

  def listEncoder[T](implicit tag: WeakTypeTag[T]): Tree = {
    val mapper = c.typecheck(encoderTypeMapper(tag.tpe))
    val casTpe = function2Types(mapper)._2
    c.typecheck {
      q"""
       val listMapper: (List[$tag] => java.util.List[$casTpe]) = list => collection.JavaConverters
         .seqAsJavaListConverter(list.map((x: $tag) => $mapper(x))).asJava

       encoder[List[$tag]]((row: ${c.prefix}.PrepareRow) => (i: ${c.prefix}.Index, list: List[$tag]) =>
         row.setList(i, listMapper(list)))
      """
    }
  }

  def listDecoder[T](implicit tag: WeakTypeTag[T]): Tree = {
    val mapper = c.typecheck(decoderTypeMapper(tag.tpe))
    val casTpe = function2Types(mapper)._1
    c.typecheck {
      q"""
        val listMapper: (java.util.List[$casTpe] => List[$tag]) = list =>
          collection.JavaConverters.asScalaBufferConverter(list).asScala.map((x: $casTpe) => $mapper(x)).toList

        decoder((row: ${c.prefix}.ResultRow) => (i: ${c.prefix}.Index) =>
          listMapper(row.getList[$casTpe](i, classOf[$casTpe])))
      """
    }
  }
}

