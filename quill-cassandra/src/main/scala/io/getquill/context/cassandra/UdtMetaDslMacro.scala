package io.getquill.context.cassandra

import scala.reflect.macros.blackbox.{ Context => MacroContext }

class UdtMetaDslMacro(val c: MacroContext) {

  import c.universe._

  def udtMeta[T](path: Tree, columns: Tree*)(implicit t: WeakTypeTag[T]): Tree = {
    val pairs = columns.map {
      case q"(($x1) => $pack.Predef.ArrowAssoc[$t]($prop).$arrow[$v](${ alias: String }))" =>
        q"(${prop.symbol.name.decodedName.toString}, $alias)"
    }
    c.untypecheck {
      q"""
         new ${c.prefix}.UdtMeta[$t] {
           private[this] val (nm, ks) = io.getquill.context.cassandra.util.UdtMetaUtils.parse($path)
           private[this] val map = Map[String, String](..$pairs)
           def name = nm
           def keyspace = ks
           def alias(col: String) = map.get(col)
         }
       """
    }
  }
}
