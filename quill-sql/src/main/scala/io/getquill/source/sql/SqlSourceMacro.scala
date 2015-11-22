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

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, Literal => _, _ }

  override def toExecutionTree(ast: Ast) = {
    implicit val (d, n) = dialectAndNaming
    val sql = show(ast)
    c.info(sql)
    probe(sql, d)
    q"$sql"
  }

  private def show(ast: Ast)(implicit d: SqlIdiom, n: NamingStrategy) = {
    import d._
    ast match {
      case ast: Query =>
        val sql = SqlQuery(Normalize(ExpandOuterJoin(ast)))
        VerifySqlQuery(sql).map(c.fail)
        ExpandNestedQueries(sql, Set.empty).show
      case ast =>
        ast.show
    }
  }

  private def probe(sql: String, d: SqlIdiom) =
    resolveSource[SqlSource[SqlIdiom, NamingStrategy, Any, Any]].map {
      _.probe(d.prepare(sql))
    } match {
      case Some(Failure(e)) => c.error(s"The sql query probing failed. Reason '$e'")
      case other            =>
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
