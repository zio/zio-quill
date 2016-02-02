package io.getquill.sources

import scala.annotation.StaticAnnotation
import scala.concurrent.duration.DurationInt
import scala.language.existentials
import scala.reflect.api.Types
import scala.reflect.macros.whitebox.Context
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import io.getquill.util.Messages._
import io.getquill.NoQueryProbing

import org.scalamacros.resetallattrs.ResetAllAttrs

import io.getquill.util.Cache

case class QuotedSource(tree: Any, probingEnabled: Boolean) extends StaticAnnotation

trait ResolveSourceMacro {
  val c: Context
  import c.universe.{ Try => _, _ }

  def quoteSource[T <: Source[_, _]](config: Expr[SourceConfig[T]])(implicit t: WeakTypeTag[T]) = {
    val probingEnabled = !(config.actualType <:< c.weakTypeOf[NoQueryProbing])
    q"""
      new $t($config) {
        @${c.weakTypeOf[QuotedSource]}(new $t($config), $probingEnabled)
        def quoted() = ()
      }  
    """
  }

  def resolveSource[T <: Source[_, _]]: Option[T] = {
    val tpe = c.prefix.tree.tpe
    resolveSource(tpe, sourceTree(tpe))
      .asInstanceOf[Option[T]]
  }

  private def resolveSource(tpe: Type, sourceTree: Option[List[Tree]]): Option[Any] = {
    val tpe = c.prefix.tree.tpe
    ResolveSourceMacro.cache
      .getOrElseUpdate(tpe, unquote(sourceTree), 30.seconds)
  }

  private def unquote(sourceTree: Option[List[Tree]]): Option[Source[_, _]] =
    sourceTree match {
      case Some(_ :: q"false" :: Nil) =>
        None
      case Some(tree :: _ :: Nil) =>
        loadSource(tree.duplicate) match {
          case Success(value) =>
            Some(value)
          case Failure(exception) =>
            c.warn(s"Can't load source at compile time. Reason: '${exception.getMessage}'.")
            None
        }
      case o =>
        c.warn("Can't load source at compile time. Query probing disabled.")
        None
    }

  private def loadSource(tree: Tree) = {
    val t =
      q"""
        import scala.reflect.ClassTag
        import io.getquill.naming._
        import io.getquill._
        $tree  
      """
    eval(t).orElse(eval(t))
  }

  private def eval(tree: Tree) = {
    import org.scalamacros.resetallattrs._
    Try(c.eval[Source[_, _]](c.Expr(c.resetAllAttrs(tree))))
  }

  private def sourceTree(tpe: Type) =
    for {
      method <- tpe.decls.find(_.name.decodedName.toString == "quoted")
      annotation <- method.annotations.headOption
    } yield annotation.tree.children.drop(1) 
}

object ResolveSourceMacro {
  private val cache = new Cache[Types#Type, Source[_, _]]
}
