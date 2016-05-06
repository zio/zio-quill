package io.getquill.sources

import scala.reflect.macros.whitebox.Context

import io.getquill._
import io.getquill.ast._
import io.getquill.util.Messages.fail

trait ActionMacro extends EncodingMacro {
  this: SourceMacro =>

  val c: Context
  import c.universe.{ Ident => _, _ }

  def runAction[S, T](
    quotedTree:     Tree,
    action:         Ast,
    inPlaceParams:  collection.Map[Ident, (c.Type, c.Tree)],
    functionParams: List[(Ident, c.Type)]
  )(
    implicit
    s: WeakTypeTag[S],
    t: WeakTypeTag[T]
  ): Tree =
    functionParams match {
      case Nil =>
        val encodedParams = EncodeParams[S](c)(inPlaceParams, collection.Map())
        expandedTreeSingle(quotedTree, action, inPlaceParams.map(_._1).toList, encodedParams)

      case List((param, tpe)) if (t.tpe.erasure <:< c.weakTypeOf[UnassignedAction[Any]].erasure) =>
        val encodingValue = encoding(param, Encoding.inferEncoder[S](c))(c.WeakTypeTag(tpe))
        val bindings = bindingMap(encodingValue)
        val idents = bindings.map(_._1).toList
        val assignedAction = AssignedAction(action, idents.map(k => Assignment(Ident("x"), k.name, k)))
        val encodedParams = EncodeParams[S](c)(inPlaceParams, bindings.toMap)

        expandedTreeBatch(quotedTree, assignedAction, idents.toList ++ inPlaceParams.map(_._1), List(tpe), encodedParams)

      case functionParams =>
        val encodedParams = EncodeParams[S](c)(bindingMap(functionParams) ++ inPlaceParams, collection.Map())
        expandedTreeBatch(quotedTree, action, functionParams.map(_._1) ++ inPlaceParams.map(_._1), functionParams.map(_._2), encodedParams)
    }

  private def expandedTreeSingle(quotedTree: Tree, action: Ast, idents: List[Ident], encodedParams: Tree) = {
    q"""
    {
      val quoted = $quotedTree
      val (sql, bindings: List[io.getquill.ast.Ident], generated) =
        ${prepare(action, idents)}

      ${c.prefix}.execute(
        sql,
        $encodedParams(bindings.map(_.name)),
        generated
        )
    }
    """
  }

  private def expandedTreeBatch(quotedTree: Tree, action: Ast, idents: List[Ident], paramsTypes: List[Type], encodedParams: Tree) = {
    q"""
    {
      val quoted = $quotedTree
      val (sql, bindings: List[io.getquill.ast.Ident], generated) =
        ${prepare(action, idents)}

      ${c.prefix}.executeBatch[(..$paramsTypes)](
        sql,
        value => $encodedParams(bindings.map(_.name)),
        generated
        )
    }
    """
  }

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
