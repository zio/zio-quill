package io.getquill.context.cassandra.encoding

import scala.reflect.macros.blackbox.{ Context => MacroContext }

class MapEncodingMacro(val c: MacroContext) extends CollectionsEncodingMacro {
  import c.universe._

  def mapEncoder[K, V](implicit kT: WeakTypeTag[K], vT: WeakTypeTag[V]): Tree = {
    val kMapper = c.typecheck(encoderTypeMapper(kT.tpe))
    val vMapper = c.typecheck(encoderTypeMapper(vT.tpe))
    val casK = function2Types(kMapper)._2
    val casV = function2Types(vMapper)._2
    c.typecheck {
      q"""
       val mapMapper: (Map[$kT, $vT] => java.util.Map[$casK, $casV]) = map => collection.JavaConverters
         .mapAsJavaMapConverter(map.map((kv: ($kT, $vT)) => ($kMapper(kv._1), $vMapper(kv._2)))).asJava

       encoder[Map[$kT, $vT]]((row: ${c.prefix}.PrepareRow) => (i: ${c.prefix}.Index, map: Map[$kT, $vT]) =>
         row.setMap(i, mapMapper(map)))
      """
    }
  }

  def mapDecoder[K, V](implicit kT: WeakTypeTag[K], vT: WeakTypeTag[V]): Tree = {
    val kMapper = c.typecheck(decoderTypeMapper(kT.tpe))
    val vMapper = c.typecheck(decoderTypeMapper(vT.tpe))
    val casK = function2Types(kMapper)._1
    val casV = function2Types(vMapper)._1
    c.typecheck {
      q"""
        val mapMapper: (java.util.Map[$casK, $casV] => Map[$kT, $vT]) = map => collection.JavaConverters
          .mapAsScalaMapConverter(map).asScala.map((kv: ($casK, $casV)) => ($kMapper(kv._1), $vMapper(kv._2))).toMap

        decoder((row: ${c.prefix}.ResultRow) => (i: ${c.prefix}.Index) =>
          mapMapper(row.getMap(i, classOf[$casK], classOf[$casV])))
      """
    }
  }
}
