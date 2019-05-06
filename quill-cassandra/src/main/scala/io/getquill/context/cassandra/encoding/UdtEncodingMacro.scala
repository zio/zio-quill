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
  private def convert = q"scala.collection.JavaConverters"

  def udtDecoder[T](implicit t: WeakTypeTag[T]): Tree = {
    val (typeDefs, params, getters) = decodeUdt(t.tpe)
    c.untypecheck {
      q"""
         ${buildUdtMeta(t.tpe)}
         def udtdecoder[..$typeDefs](implicit ..$params): $prefix.Decoder[${t.tpe}] = {
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
         def udtdecodemapper[..$typeDefs](implicit ..$params): $encoding.CassandraMapper[$udtRaw, ${t.tpe}] = {
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
         def udtencoder[..$typeDefs](implicit ..$params): $prefix.Encoder[${t.tpe}] = {
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
         def udtencodemapper[..$typeDefs](implicit ..$params): $encoding.CassandraMapper[${t.tpe}, $udtRaw] = {
           $encoding.CassandraMapper(x => {..$body})
         }
         udtencodemapper
       """
    }
  }

  private def decodeUdt[T](udtType: Type) = {
    val t = udtFields(udtType).map {
      case (name, _, tpe, mapper, absType, absTypeDef, tag) =>

        def classTree = q"$tag.runtimeClass.asInstanceOf[Class[$absType]]"

        def tagTree = q"$tag: scala.reflect.ClassTag[$absType]"

        def rawTree = q"$mapper.f(udt.get[$absType]($name, $classTree))"

        val params = ListBuffer.empty[Tree]
        val typeDefs = ListBuffer.empty[TypeDef]
        typeDefs.append(absTypeDef)

        def single(tpe: Type) = q"$mapper: $encoding.CassandraMapper[$absType, $tpe]"

        val tree =
          if (tpe <:< typeOf[Option[Any]]) {
            params.append(single(tpe.typeArgs.head), tagTree)
            q"Option($rawTree)"

          } else if (tpe <:< typeOf[List[Any]]) {
            params.append(single(tpe.typeArgs.head), tagTree)
            val list = q"udt.getList[$absType]($name, $classTree)"
            q"$convert.asScalaBufferConverter($list).asScala.map($mapper.f).toList"

          } else if (isBaseType[Set[Any]](tpe)) {
            params.append(single(tpe.typeArgs.head), tagTree)
            val set = q"udt.getSet[$absType]($name, $classTree)"
            q"$convert.asScalaSetConverter($set).asScala.map($mapper.f).toSet"

          } else if (isBaseType[collection.Map[Any, Any]](tpe)) {
            val vAbsName = s"${absTypeDef.name.encodedName}V"
            val vAbsType = TypeName(vAbsName)
            typeDefs.append(makeTypeDef(vAbsName))
            val vTag = TermName(s"${tag.encodedName}V")
            val kTpe :: vTpe :: Nil = tpe.typeArgs
            val vMapper = TermName(s"${mapper.encodedName}V")
            params.append(
              q"$mapper: $encoding.CassandraMapper[$absType, $kTpe]",
              q"$vMapper: $encoding.CassandraMapper[$vAbsType, $vTpe]",
              tagTree,
              q"$vTag: scala.reflect.ClassTag[$vAbsType]"
            )
            val map = q"udt.getMap[$absType, $vAbsType]($name, $classTree, $vTag.runtimeClass.asInstanceOf[Class[$vAbsType]])"
            q"$convert.mapAsScalaMapConverter($map).asScala.map(kv => $mapper.f(kv._1) -> $vMapper.f(kv._2)).toMap"

          } else {
            params.append(single(tpe), tagTree)
            rawTree
          }
        (typeDefs.toList, params.toList, tree)
    }.unzip3
    t.copy(_1 = t._1.flatten, _2 = t._2.flatten)
  }

  private def encodeUdt[T](udtType: Type) = {
    val trees = ListBuffer[Tree](q"val udt = $prefix.udtValueOf(meta.name, meta.keyspace)")
    val (typeDefs, params) = udtFields(udtType).map {
      case (name, field, tpe, mapper, absType, absTypeDef, tag) =>

        val params = ListBuffer.empty[Tree]
        val typeDefs = ListBuffer.empty[TypeDef]
        typeDefs.append(absTypeDef)

        def classTree = q"$tag.runtimeClass.asInstanceOf[Class[$absType]]"

        def tagTree = q"$tag: scala.reflect.ClassTag[$absType]"

        def single(tpe: Type) = q"$mapper: $encoding.CassandraMapper[$tpe, $absType]"

        if (tpe <:< typeOf[Option[Any]]) {
          params.append(single(tpe.typeArgs.head), tagTree)
          trees.append {
            q"x.$field.map(v => udt.set[$absType]($name, $mapper.f(v), $classTree)).getOrElse(udt.setToNull($name))"
          }
        } else if (tpe <:< typeOf[List[Any]]) {
          params.append(single(tpe.typeArgs.head), tagTree)
          val list = q"x.$field.map($mapper.f)"
          trees.append {
            q"udt.setList[$absType]($name, $convert.seqAsJavaListConverter($list).asJava, $classTree)"
          }
        } else if (isBaseType[Set[Any]](tpe)) {
          params.append(single(tpe.typeArgs.head), tagTree)
          val set = q"$convert.setAsJavaSetConverter(x.$field.map($mapper.f)).asJava"
          trees.append {
            q"udt.setSet[$absType]($name, $set, $classTree)"
          }
        } else if (isBaseType[collection.Map[Any, Any]](tpe)) {
          val vAbsName = s"${absTypeDef.name.encodedName}V"
          val vAbsType = TypeName(vAbsName)
          typeDefs.append(makeTypeDef(vAbsName))
          val vTag = TermName(s"${tag.encodedName}V")
          val kTpe :: vTpe :: Nil = tpe.typeArgs
          val vMapper = TermName(s"${mapper.encodedName}V")
          params.append(
            q"$mapper: $encoding.CassandraMapper[$kTpe, $absType]",
            q"$vMapper: $encoding.CassandraMapper[$vTpe, $vAbsType]",
            tagTree,
            q"$vTag: scala.reflect.ClassTag[$vAbsType]"
          )
          val vClassTree = q"$vTag.runtimeClass.asInstanceOf[Class[$vAbsType]]"
          val map = q"$convert.mapAsJavaMapConverter(x.$field.map(kv => $mapper.f(kv._1) -> $vMapper.f(kv._2))).asJava"
          trees.append {
            q"udt.setMap[$absType, $vAbsType]($name, $map, $classTree, $vClassTree)"
          }
        } else {
          params.append(single(tpe), tagTree)
          trees.append {
            q"udt.set[$absType]($name, $mapper.f(x.$field), $classTree)"
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

    fields.zipWithIndex.map {
      case (f, i) =>
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
