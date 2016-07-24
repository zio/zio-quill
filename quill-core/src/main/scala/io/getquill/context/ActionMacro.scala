package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }

import io.getquill.ast.AssignedAction
import io.getquill.ast.Assignment
import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.ast.Property
import io.getquill.dsl.CoreDsl
import io.getquill.util.Messages.fail

trait ActionMacro extends EncodingMacro {
  this: ContextMacro =>

  val c: MacroContext
  import c.universe.{ Ident => _, _ }

  def runAction[R, S, T](
    quotedTree:     Tree,
    action:         Ast,
    inPlaceParams:  collection.Map[Ident, (c.Type, c.Tree)],
    functionParams: List[(Ident, c.Type)]
  )(
    implicit
    r: WeakTypeTag[R],
    s: WeakTypeTag[S],
    t: WeakTypeTag[T]
  ): Tree = {

    functionParams match {
      case Nil =>
        val encodedParams = EncodeParams[S](c)(inPlaceParams, collection.Map())
        expandedTreeSingle[R](quotedTree, action, inPlaceParams.map(_._1).toList, encodedParams, returningType(t.tpe))

      case List((param, tpe)) if (t.tpe.erasure <:< c.weakTypeOf[CoreDsl#UnassignedAction[Any, Any]].erasure) =>
        val encodingValue = encoding(param, Encoding.inferEncoder[S](c))(c.WeakTypeTag(tpe))
        val bindings = bindingMap(encodingValue)
        val idents = bindings.map(_._1).toList
        val assignedAction = AssignedAction(action, idents.map(k => Assignment(Ident("x"), k.name, k)))
        val encodedParams = EncodeParams[S](c)(inPlaceParams, bindings.toMap)

        expandedTreeBatch[R](quotedTree, assignedAction, idents.toList ++ inPlaceParams.map(_._1), List(tpe), encodedParams, returningType(t.tpe))

      case functionParams =>
        val encodedParams = EncodeParams[S](c)(bindingMap(functionParams) ++ inPlaceParams, collection.Map())
        expandedTreeBatch[R](quotedTree, action, functionParams.map(_._1) ++ inPlaceParams.map(_._1), functionParams.map(_._2), encodedParams, returningType(t.tpe))
    }
  }

  private def returningExtractor[R](returnType: c.Type)(implicit r: WeakTypeTag[R]) = {
    val returnWeakTypeTag = c.WeakTypeTag(returnType)
    val selectValues = encoding(Ident("X"), Encoding.inferDecoder[R](c))(returnWeakTypeTag)
    selectResultExtractor[R](selectValues)
  }

  private def expandedTreeSingle[R](quotedTree: Tree, action: Ast, idents: List[Ident], encodedParams: Tree, returningType: Type)(implicit r: WeakTypeTag[R]) = {
    q"""
    {
      val quoted = $quotedTree
      val (sql, bindings: List[io.getquill.ast.Ident], returning) =
        ${prepare(action, idents)}

      ${c.prefix}.executeAction[$returningType](
        sql,
        $encodedParams(bindings.map(_.name)),
        returning,
        ${returningExtractor(returningType)(r)}
        )
    }
    """
  }

  private def expandedTreeBatch[R](quotedTree: Tree, action: Ast, idents: List[Ident], paramsTypes: List[Type], encodedParams: Tree, returningType: Type)(implicit r: WeakTypeTag[R]) = {
    q"""
    {
      val quoted = $quotedTree
      val (sql, bindings: List[io.getquill.ast.Ident], returning) =
        ${prepare(action, idents)}

      ${c.prefix}.executeActionBatch[(..$paramsTypes), $returningType](
        sql,
        value => $encodedParams(bindings.map(_.name)),
        returning,
        ${returningExtractor(returningType)(r)}
        )
    }
    """
  }

  private def returningType(tpe: Type): Type = tpe.baseType(c.typeOf[CoreDsl#Action[_, _]].typeSymbol).typeArgs(1)

  private def bindingMap(value: Value, option: Boolean = false): List[(Ident, (Tree, Tree))] =
    value match {
      case CaseClassValue(tpe, values) =>
        values.flatten.map(bindingMap(_)).flatten
      case OptionValue(value) =>
        bindingMap(value, option = true)
      case SimpleValue(ast, encoder, optionEncoder) =>
        val (ident, tree) = bindingTree(ast)
        val enc = if (option) optionEncoder else encoder
        List((ident, (enc, tree)))
    }

  private def bindingTree(ast: Ast): (Ident, Tree) =
    ast match {
      case Property(ast: Property, name) =>
        val (ident, tree) = bindingTree(ast)
        (ident, q"$tree.${TermName(name)}")
      case Property(ast: Ident, name) =>
        (Ident(name), q"value.${TermName(name)}")
      case other =>
        fail(s"Can't bind '$other'.")
    }

  private def bindingMap(params: List[(Ident, Type)]): collection.Map[Ident, (Type, Tree)] =
    params match {
      case (param, tpe) :: Nil =>
        collection.Map((param, (tpe, q"value")))
      case params =>
        (for (((param, tpe), index) <- params.zipWithIndex) yield {
          (param, (tpe, q"value.${TermName(s"_${index + 1}")}"))
        }).toMap
    }
}
