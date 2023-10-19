package io.getquill.context.cassandra.encoding

import com.datastax.oss.driver.api.core.data.UdtValue
import io.getquill.util.MacroContextExt._
import io.getquill.util.OptionalTypecheck

import scala.collection.mutable.ListBuffer
import scala.reflect.macros.blackbox.{Context => MacroContext}

class UdtEncodingMacro(val c: MacroContext) {

  import c.universe._

  private val encoding    = q"io.getquill.context.cassandra.encoding"
  private val udtRaw      = typeOf[UdtValue]
  private def prefix      = c.prefix
  private def UdtValueOps = q"_root_.io.getquill.context.cassandra.encoding.UdtValueOps"

  def udtDecoder[T](implicit t: WeakTypeTag[T]): Tree = {
    val (typeDefs, params, getters) = decodeUdt(t.tpe)
    c.untypecheck {
      q"""
         ${buildUdtMeta(t.tpe)}
         def udtdecoder[..$typeDefs](implicit ..$params): $prefix.Decoder[${t.tpe}] = {
           $prefix.decoder { (index, row, session) =>
             val udt = row.getUdtValue(index)
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
         def udtdecodemapper[..$typeDefs](implicit ..$params): $encoding.CassandraMapper[$udtRaw, ${t.tpe}] = {
           $encoding.CassandraMapper((udt, session) => new ${t.tpe}(..$getters))
         }
         udtdecodemapper
       """
    }
  }

  def udtEncoder[T](implicit t: WeakTypeTag[T]): Tree = {
    val (typeDefs, params, body) = encodeUdt(t.tpe)
    val swapin =
      c.untypecheck {
        q"""
           ${buildUdtMeta(t.tpe)}
           def udtencoder[..$typeDefs](implicit ..$params): $prefix.Encoder[${t.tpe}] = {
             $prefix.encoder[$t]((i: $prefix.Index, x: ${t.tpe}, row: $prefix.PrepareRow, session: Session) => row.setUdtValue(i, {..$body}))
           }
           udtencoder
         """
      }

    // println("=========== SWAPIN udtEncoder ===========\n" + show(swapin))
    swapin
  }

  def udtEncodeMapper[T](implicit t: WeakTypeTag[T]): Tree = {
    val (typeDefs, params, body) = encodeUdt(t.tpe)
    val swapin =
      c.untypecheck {
        q"""
           ${buildUdtMeta(t.tpe)}
           def udtencodemapper[..$typeDefs](implicit ..$params): $encoding.CassandraMapper[${t.tpe}, $udtRaw] = {
             $encoding.CassandraMapper((x, session) => {..$body})
           }
           udtencodemapper
         """
      }
    // println("=========== SWAPIN udtEncodeMapper ===========\n" + show(swapin))
    swapin
  }

  private def decodeUdt[T](udtType: Type) = {
    val t = udtFields(udtType).map { case (name, _, tpe, mapper, absType, absTypeDef, tag) =>
      def classTree = q"$tag.runtimeClass.asInstanceOf[Class[$absType]]"

      def tagTree = q"$tag: scala.reflect.ClassTag[$absType]"

      def rawTree = q"$mapper.f(udt.get[$absType]($name, $classTree), session)"

      val params   = ListBuffer.empty[Tree]
      val typeDefs = ListBuffer.empty[TypeDef]
      typeDefs.append(absTypeDef)

      def single(tpe: Type) = q"$mapper: $encoding.CassandraMapper[$absType, $tpe]"

      val tree =
        if (tpe <:< typeOf[Option[Any]]) {
          params.appendAll(Seq(single(tpe.typeArgs.head), tagTree))
          q"Option($rawTree)"

        } else if (tpe <:< typeOf[List[Any]]) {
          params.appendAll(Seq(single(tpe.typeArgs.head), tagTree))
          val list = q"$UdtValueOps(udt).getScalaList[$absType]($name, $classTree)"
          q"$list.map(row => $mapper.f(row, session)).toList"

        } else if (isBaseType[Set[Any]](tpe)) {
          params.appendAll(Seq(single(tpe.typeArgs.head), tagTree))
          val set = q"$UdtValueOps(udt).getScalaSet[$absType]($name, $classTree)"
          q"$set.map(row => $mapper.f(row, session)).toSet"

        } else if (isBaseType[collection.Map[Any, Any]](tpe)) {
          val vAbsName = s"${absTypeDef.name.encodedName}V"
          val vAbsType = TypeName(vAbsName)
          typeDefs.append(makeTypeDef(vAbsName))
          val vTag                = TermName(s"${tag.encodedName}V")
          val kTpe :: vTpe :: Nil = tpe.typeArgs
          val vMapper             = TermName(s"${mapper.encodedName}V")
          params.appendAll(
            Seq(
              q"$mapper: $encoding.CassandraMapper[$absType, $kTpe]",
              q"$vMapper: $encoding.CassandraMapper[$vAbsType, $vTpe]",
              tagTree,
              q"$vTag: scala.reflect.ClassTag[$vAbsType]"
            )
          )
          val map =
            q"$UdtValueOps(udt).getScalaMap[$absType, $vAbsType]($name, $classTree, $vTag.runtimeClass.asInstanceOf[Class[$vAbsType]])"
          q"$map.map(kv => $mapper.f(kv._1, session) -> $vMapper.f(kv._2, session)).toMap"

        } else {
          params.appendAll(Seq(single(tpe), tagTree))
          rawTree
        }
      (typeDefs.toList, params.toList, tree)
    }.unzip3
    t.copy(_1 = t._1.flatten, _2 = t._2.flatten)
  }

  private def encodeUdt[T](udtType: Type) = {
    // The `session` variable represents CassandraSession which will either be `this` (if it is CassandraClusterSessionContext)
    // or it will be `CassandraZioSession` otherwise. Either way, it should have the `udtValueOf` method.
    // It is passed in via the context.encoder (i.e. $prefix.encoder) variable
    val trees = ListBuffer[Tree](q"val udt = session.udtValueOf(meta.name, meta.keyspace)")
    val (typeDefs, params) = udtFields(udtType).map { case (name, field, tpe, mapper, absType, absTypeDef, tag) =>
      val params   = ListBuffer.empty[Tree]
      val typeDefs = ListBuffer.empty[TypeDef]
      typeDefs.append(absTypeDef)

      def classTree = q"$tag.runtimeClass.asInstanceOf[Class[$absType]]"

      def tagTree = q"$tag: scala.reflect.ClassTag[$absType]"

      def single(tpe: Type) = q"$mapper: $encoding.CassandraMapper[$tpe, $absType]"

      if (tpe <:< typeOf[Option[Any]]) {
        params.appendAll(Seq(single(tpe.typeArgs.head), tagTree))
        trees.append {
          q"x.$field.map(v => udt.set[$absType]($name, $mapper.f(v, session), $classTree)).getOrElse(udt.setToNull($name))"
        }
      } else if (tpe <:< typeOf[List[Any]]) {
        params.appendAll(Seq(single(tpe.typeArgs.head), tagTree))
        val list = q"x.$field.map(row => $mapper.f(row, session))"
        trees.append {
          q"$UdtValueOps(udt).setScalaList[$absType]($name, $list, $classTree)"
        }
      } else if (isBaseType[Set[Any]](tpe)) {
        params.appendAll(Seq(single(tpe.typeArgs.head), tagTree))
        val set = q"x.$field.map(row => $mapper.f(row, session))"
        trees.append {
          q"$UdtValueOps(udt).setScalaSet[$absType]($name, $set, $classTree)"
        }
      } else if (isBaseType[collection.Map[Any, Any]](tpe)) {
        val vAbsName = s"${absTypeDef.name.encodedName}V"
        val vAbsType = TypeName(vAbsName)
        typeDefs.append(makeTypeDef(vAbsName))
        val vTag                = TermName(s"${tag.encodedName}V")
        val kTpe :: vTpe :: Nil = tpe.typeArgs
        val vMapper             = TermName(s"${mapper.encodedName}V")
        params.appendAll(
          Seq(
            q"$mapper: $encoding.CassandraMapper[$kTpe, $absType]",
            q"$vMapper: $encoding.CassandraMapper[$vTpe, $vAbsType]",
            tagTree,
            q"$vTag: scala.reflect.ClassTag[$vAbsType]"
          )
        )
        val vClassTree = q"$vTag.runtimeClass.asInstanceOf[Class[$vAbsType]]"
        val map        = q"x.$field.map(kv => $mapper.f(kv._1, session) -> $vMapper.f(kv._2, session))"
        trees.append {
          q"$UdtValueOps(udt).setScalaMap[$absType, $vAbsType]($name, $map, $classTree, $vClassTree)"
        }
      } else {
        params.appendAll(Seq(single(tpe), tagTree))
        trees.append {
          q"udt.set[$absType]($name, $mapper.f(x.$field, session), $classTree)"
        }
      }
      typeDefs.toList -> params.toList
    }.unzip
    (typeDefs.flatten, params.flatten, trees.toList)
  }

  private def udtFields(udt: Type) = {
    val fields = udt.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m.paramLists.flatten
    }.getOrElse(c.fail(s"Could not find primary constructor of $udt"))

    fields.zipWithIndex.map { case (f, i) =>
      val name = f.name.decodedName.toString
      (
        q"meta.alias($name).getOrElse($prefix.naming.default($name))",
        f.name.toTermName,
        f.typeSignature.asSeenFrom(udt, udt.typeSymbol),
        TermName(s"m$i"),
        TypeName(s"T$i"),
        makeTypeDef(s"T$i"),
        TermName(s"t$i")
      )
    }
  }

  private def isBaseType[T](tpe: Type)(implicit t: TypeTag[T]) =
    !(tpe.baseType(t.tpe.typeSymbol) =:= NoType)

  private def makeTypeDef(name: String) =
    TypeDef(Modifiers(Flag.PARAM), TypeName(name), Nil, TypeBoundsTree(EmptyTree, EmptyTree))

  private def buildUdtMeta(tpe: Type): Tree = {
    val meta = OptionalTypecheck(c)(q"implicitly[$prefix.UdtMeta[$tpe]]") getOrElse {
      q"$prefix.udtMeta[$tpe]($prefix.naming.default(${typeName(tpe)}))"
    }
    c.typecheck(q"val meta: $prefix.UdtMeta[$tpe] = $meta")
  }

  private def typeName(tpe: Type): String = tpe.typeSymbol.name.decodedName.toString
}
