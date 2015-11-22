package io.getquill.source.sql

import scala.reflect.macros.whitebox.Context
import scala.util.Failure
import scala.util.Success
import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.source.SourceMacro
import io.getquill.source.sql.idiom.SqlIdiom
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower
import io.getquill.quotation.Quoted
import scala.reflect.ClassTag
import io.getquill.source.sql.idiom.SqlIdiom
import io.getquill.norm.Normalize
import io.getquill.quotation.IsDynamic
import io.getquill.source.BindVariables

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, Literal => _, Ident => _, _ }

  override protected def prepare(ast: Ast, params: List[Ident]) = {
    implicit val (d, n) = dialectAndNaming
    if (!IsDynamic(ast)) {
      val (sql, idents) = Prepare(ast, params)
      c.info(sql)
      probe(sql, d)
      q"($sql, $idents)"
    } else {
      q"""
      {
        implicit val (d, n) = $dialectAndNamingDynamic
        io.getquill.source.sql.Prepare($ast, $params)
      }
      """
    }
  }

  private def probe(sql: String, d: SqlIdiom) =
    resolveSource[SqlSource[SqlIdiom, NamingStrategy, Any, Any]].map {
      _.probe(d.prepare(sql))
    } match {
      case Some(Failure(e)) => c.error(s"The sql query probing failed. Reason '$e'")
      case other            =>
    }

  private def dialectAndNamingDynamic = {
    val (idiom :: n :: _) =
      c.prefix.actualType
        .baseType(c.weakTypeOf[SqlSource[SqlIdiom, NamingStrategy, Any, Any]].typeSymbol)
        .typeArgs
    val types =
      n match {
        case RefinedType(types, _) => types
        case other                 => List(other)
      }
    val objs =
      types
        .filterNot(_ =:= c.weakTypeOf[NamingStrategy])
        .filterNot(_ =:= c.weakTypeOf[scala.Nothing])
        .map(_.typeSymbol.companion)
    val naming =
      q"""
      new io.getquill.naming.NamingStrategy {
        override def apply(s: String) = {
          ${objs.foldLeft[Tree](q"s")((s, n) => q"$n($s)")}
        }
      }
      """
    q"(${idiom.typeSymbol.companion}, $naming)"
  }

  private def dialectAndNaming = {
    val (idiom :: n :: _) =
      c.prefix.actualType
        .baseType(c.weakTypeOf[SqlSource[SqlIdiom, NamingStrategy, Any, Any]].typeSymbol)
        .typeArgs
    val types =
      n match {
        case RefinedType(types, _) => types
        case other                 => List(other)
      }
    val objs =
      types
        .filterNot(_ =:= c.weakTypeOf[NamingStrategy])
        .filterNot(_ =:= c.weakTypeOf[scala.Nothing])
        .map(loadObject[NamingStrategy])
    val naming =
      new NamingStrategy {
        override def apply(s: String) =
          objs.foldLeft(s)((s, n) => n(s))
      }
    (loadObject[SqlIdiom](idiom), naming)
  }

  private def loadObject[T: ClassTag](typ: Type) = {
    loadClass(typ.typeSymbol.fullName.trim + "$")
      .map(cls => cls.getField("MODULE$").get(cls)) match {
        case Some(d: T) => d
        case other      => c.fail(s"Can't load object '${typ.typeSymbol.fullName}'.")
      }
  }
}
