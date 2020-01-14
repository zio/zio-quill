package io.getquill.dsl

import io.getquill.util.OptionalTypecheck
import io.getquill.util.MacroContextExt._
import scala.reflect.macros.blackbox.{ Context => MacroContext }

class EncodingDslMacro(val c: MacroContext) {
  import c.universe._

  def anyValEncoder[T](implicit t: WeakTypeTag[T]): Tree =
    withAnyValParam(t.tpe) { param =>
      q"""
        ${c.prefix}.mappedEncoder(
          io.getquill.MappedEncoding((v: ${t.tpe}) => v.${param.name.toTermName}),
          implicitly[${c.prefix}.Encoder[${param.typeSignature}]]
        )
      """
    }.getOrElse(fail("Encoder", t.tpe))

  def anyValDecoder[T](implicit t: WeakTypeTag[T]): Tree =
    withAnyValParam(t.tpe) { param =>
      q"""
        ${c.prefix}.mappedDecoder(
          io.getquill.MappedEncoding((p: ${param.typeSignature}) => new ${t.tpe}(p)),
          implicitly[${c.prefix}.Decoder[${param.typeSignature}]]
        )
      """
    }.getOrElse(fail("Decoder", t.tpe))

  def lift[T](v: Tree)(implicit t: WeakTypeTag[T]): Tree =
    lift[T](v, "lift")

  def liftQuery[T](v: Tree)(implicit t: WeakTypeTag[T]): Tree =
    lift[T](v, "liftQuery")

  private def lift[T](v: Tree, method: String)(implicit t: WeakTypeTag[T]): Tree =
    OptionalTypecheck(c)(q"implicitly[${c.prefix}.Encoder[$t]]") match {
      case Some(enc) =>
        q"${c.prefix}.${TermName(s"${method}Scalar")}($v)($enc)"
      case None =>
        t.tpe.baseType(c.symbolOf[Product]) match {
          case NoType => fail("Encoder", t.tpe)
          case _ =>
            q"${c.prefix}.${TermName(s"${method}CaseClass")}($v)"
        }
    }

  private def fail(enc: String, t: Type) =
    c.fail(s"Can't find $enc for type '$t'. Note that ${enc}s are invariant. For example, use `lift(Option(1))` instead of `lift(Some(1))` since the available encoder is for `Option`, not `Some`. As an alternative for types that don't provide a method like `Option.apply`, you can use type widening: `lift(MyEnum.SomeValue: MyEnum.Value)`")

  private def withAnyValParam[R](tpe: Type)(f: Symbol => R): Option[R] =
    tpe.baseType(c.symbolOf[AnyVal]) match {
      case NoType => None
      case _ =>
        primaryConstructor(tpe).map(_.paramLists.flatten).collect {
          case param :: Nil => f(param)
        }
    }

  private def primaryConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if m.isPrimaryConstructor => m.typeSignature.asSeenFrom(t, t.typeSymbol)
    }.headOption
}
