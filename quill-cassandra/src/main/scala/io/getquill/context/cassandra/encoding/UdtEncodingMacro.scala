package io.getquill.context.cassandra.encoding

import com.datastax.driver.core.UDTValue
import io.getquill.util.Messages._
import io.getquill.util.OptionalTypecheck

import scala.collection.mutable.ListBuffer
import scala.reflect.macros.blackbox.{ Context => MacroContext }

class UdtEncodingMacro(val c: MacroContext) {

  import c.universe._

  private val encoding = q"io.getquill.context.cassandra.encoding"
  private val udtRaw = typeOf[UDTValue]
  private def prefix = c.prefix

  def udtDecoder[T](implicit t: WeakTypeTag[T]): Tree = {
    val (typeDefs, params, getters) = decodeUdt(t.tpe)
    c.untypecheck {
      q"""
         ${buildUdtMeta(t.tpe)}
         def udtdecoder[..$typeDefs](implicit ..${params.flatten}): $prefix.Decoder[${t.tpe}] = {
           $prefix.decoder { (index, row) =>
             val udt = row.getUDTValue(index)
             new ${t.tpe}(..$getters)
           }
         }
         udtdecoder
       """
    }
  }

  def udtDecodeMapper[T](implicit t: WeakTypeTag[T]): Tree = {
    val (typeDefs, params, getters) = decodeUdt(t.tpe)
    c.untypecheck {
      q"""
         ${buildUdtMeta(t.tpe)}
         def udtdecodemapper[..$typeDefs](implicit ..${params.flatten}): $encoding.CassandraMapper[$udtRaw, ${t.tpe}] = {
           $encoding.CassandraMapper(udt => new ${t.tpe}(..$getters))
         }
         udtdecodemapper
       """
    }
  }

  def udtEncoder[T](implicit t: WeakTypeTag[T]): Tree = {
    val (typeDefs, params, body) = encodeUdt(t.tpe)
    c.untypecheck {
      q"""
         ${buildUdtMeta(t.tpe)}
         def udtencoder[..$typeDefs](implicit ..${params.flatten}): $prefix.Encoder[${t.tpe}] = {
           $prefix.encoder[$t]((i: $prefix.Index, x: ${t.tpe}, row: $prefix.PrepareRow) => row.setUDTValue(i, {..$body}))
         }
         udtencoder
       """
    }
  }

  def udtEncodeMapper[T](implicit t: WeakTypeTag[T]): Tree = {
    val (typeDefs, params, body) = encodeUdt(t.tpe)
    c.untypecheck {
      q"""
         ${buildUdtMeta(t.tpe)}
         def udtencodemapper[..$typeDefs](implicit ..${params.flatten}): $encoding.CassandraMapper[${t.tpe}, $udtRaw] = {
           $encoding.CassandraMapper(x => {..$body})
         }
         udtencodemapper
       """
    }
  }

  private def decodeUdt[T](udtType: Type) = {
    udtFields(udtType).map {
      case (name, _, tpe, mapper, absType, absTypeDef, tag) =>
        val getTree = q"$mapper.f(udt.get[$absType]($name, $tag.runtimeClass.asInstanceOf[Class[$absType]]))"
        val (newTpe, tree) =
          if (tpe <:< typeOf[Option[Any]]) tpe.typeArgs.head -> q"Option($getTree)"
          else tpe -> getTree
        val params = List(q"$mapper: $encoding.CassandraMapper[$absType, $newTpe]", q"$tag: scala.reflect.ClassTag[$absType]")
        (absTypeDef, params, tree)
    }.unzip3
  }

  private def encodeUdt[T](udtType: Type) = {
    val list = ListBuffer[Tree](q"val udt = $prefix.udtValueOf(meta.name, meta.keyspace)")
    val (typeDefs, params) = udtFields(udtType).map {
      case (name, field, tpe, mapper, absType, absTypeDef, tag) =>
        val newTpe =
          if (tpe <:< typeOf[Option[Any]]) {
            list.append {
              q"""
               x.$field.map($mapper.f) match {
                  case None => udt.setToNull($name)
                  case Some(value) => udt.set[$absType]($name, value, $tag.runtimeClass.asInstanceOf[Class[$absType]])
               }
             """
            }
            tpe.typeArgs.head
          } else {
            list.append(q"udt.set[$absType]($name, $mapper.f(x.$field), $tag.runtimeClass.asInstanceOf[Class[$absType]])")
            tpe
          }
        absTypeDef -> List(
          q"$mapper: $encoding.CassandraMapper[$newTpe, $absType]",
          q"$tag: scala.reflect.ClassTag[$absType]"
        )
    }.unzip
    (typeDefs, params, list.toList)
  }

  private def udtFields(udt: Type) = {
    val fields = udt.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m.paramLists.flatten
    }.getOrElse(c.fail(s"Could not find primary constructor of $udt"))

    fields.zipWithIndex.map {
      case (f, i) =>
        val name = f.name.decodedName.toString
        (
          q"meta.alias($name).getOrElse($prefix.naming.default($name))",
          f.name.toTermName,
          f.typeSignature.asSeenFrom(udt, udt.typeSymbol),
          TermName(s"m$i"),
          TypeName(s"T$i"),
          TypeDef(Modifiers(Flag.PARAM), TypeName(s"T$i"), Nil, TypeBoundsTree(EmptyTree, EmptyTree)),
          TermName(s"t$i")
        )
    }
  }

  private def buildUdtMeta(tpe: Type): Tree = {
    val meta = OptionalTypecheck(c)(q"implicitly[$prefix.UdtMeta[$tpe]]") getOrElse {
      q"$prefix.udtMeta[$tpe]($prefix.naming.default(${typeName(tpe)}))"
    }
    c.typecheck(q"val meta: $prefix.UdtMeta[$tpe] = $meta")
  }

  private def typeName(tpe: Type): String = tpe.typeSymbol.name.decodedName.toString
}
