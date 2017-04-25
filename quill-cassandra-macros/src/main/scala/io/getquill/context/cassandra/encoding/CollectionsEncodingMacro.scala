package io.getquill.context.cassandra.encoding

import io.getquill.util.Messages._

import scala.reflect.macros.blackbox.{ Context => MacroContext }

trait CollectionsEncodingMacro {
  val c: MacroContext
  import c.universe._

  def encoderTypeMapper(tpe: Type): Tree = tpe match {
    case t if tpe.typeSymbol.asClass.isPrimitive      => boxPrimitive(t).getOrElse(c.fail(s"Wrong primitive $t"))
    case t if true /*TODO check if type is suitable*/ => q"identity[$t] _"
    //case _ => c.fail(s"$tpe is not suitable as a collection type")
  }

  def decoderTypeMapper(tpe: Type): Tree = tpe match {
    case t if tpe.typeSymbol.asClass.isPrimitive      => unboxPrimitive(t).getOrElse(c.fail(s"Wrong primitive $t"))
    case t if true /*TODO check if type is suitable*/ => q"identity[$t] _"
    //case _ => c.fail(s"$tpe is not suitable as a collection type")
  }

  def function2Types(mapper: Tree): (Type, Type) =
    mapper.tpe.typeArgs match {
      case a :: b :: Nil => (a, b)
      case _             => c.fail(s"Cannot parse Function2 types of given tree: $mapper")
    }

  def boxPrimitive(tpe: Type): Option[Tree] = primitives.get(tpe).map(_._1)
  def unboxPrimitive(tpe: Type): Option[Tree] = primitives.get(tpe).map(_._2)

  private val primitives: Map[Type, (Tree, Tree)] = Map(
    (definitions.IntTpe, q"int2Integer _" -> q"Integer2int _"),
    (definitions.LongTpe, q"long2Long _" -> q"Long2long _"),
    (definitions.FloatTpe, q"float2Float _" -> q"Float2float _"),
    (definitions.DoubleTpe, q"double2Double _" -> q"Double2double _"),
    (definitions.BooleanTpe, q"boolean2Boolean _" -> q"Boolean2boolean _")
  )
}
