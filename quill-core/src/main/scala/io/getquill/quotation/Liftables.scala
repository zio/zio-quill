package io.getquill.quotation

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.ast
import scala.reflect.ClassTag

trait Liftables {
  val c: Context
  import c.universe.{ Ident => _, Constant => _, Function => _, _ }

  private val pack = q"io.getquill.ast"

  implicit val astLiftable: Liftable[Ast] = Liftable[Ast] {
    case ast: Query               => queryLiftable(ast)
    case ast: Action              => actionLiftable(ast)
    case ast: Value               => valueLiftable(ast)
    case ast: Ident               => identLiftable(ast)
    case Property(a, b)           => q"$pack.Property($a, $b)"
    case Function(a, b)           => q"$pack.Function($a, $b)"
    case FunctionApply(a, b)      => q"$pack.FunctionApply($a, $b)"
    case BinaryOperation(a, b, c) => q"$pack.BinaryOperation($a, $b, $c)"
    case UnaryOperation(a, b)     => q"$pack.UnaryOperation($a, $b)"
  }

  implicit val binaryOperatorLiftable: Liftable[BinaryOperator] = Liftable[BinaryOperator] {
    case ast.`-`    => q"$pack.`-`"
    case ast.`+`    => q"$pack.`+`"
    case ast.`*`    => q"$pack.`*`"
    case ast.`==`   => q"$pack.`==`"
    case ast.`!=`   => q"$pack.`!=`"
    case ast.`&&`   => q"$pack.`&&`"
    case ast.`||`   => q"$pack.`||`"
    case ast.`>`    => q"$pack.`>`"
    case ast.`>=`   => q"$pack.`>=`"
    case ast.`<`    => q"$pack.`<`"
    case ast.`<=`   => q"$pack.`<=`"
    case ast.`/`    => q"$pack.`/`"
    case ast.`%`    => q"$pack.`%`"
    case ast.`like` => q"$pack.`like`"
  }

  implicit val unaryOperatorLiftable: Liftable[UnaryOperator] = Liftable[UnaryOperator] {
    case ast.`!`        => q"$pack.`!`"
    case ast.`nonEmpty` => q"$pack.`nonEmpty`"
    case ast.`isEmpty`  => q"$pack.`isEmpty`"
  }

  implicit val queryLiftable: Liftable[Query] = Liftable[Query] {
    case Table(name)      => q"$pack.Table($name)"
    case Filter(a, b, c)  => q"$pack.Filter($a, $b, $c)"
    case Map(a, b, c)     => q"$pack.Map($a, $b, $c)"
    case FlatMap(a, b, c) => q"$pack.FlatMap($a, $b, $c)"
  }

  implicit val actionLiftable: Liftable[Action] = Liftable[Action] {
    case Update(a, b) => q"$pack.Update($a, $b)"
    case Insert(a, b) => q"$pack.Insert($a, $b)"
    case Delete(a)    => q"$pack.Delete($a)"
  }

  implicit val assignmentLiftable: Liftable[Assignment] = Liftable[Assignment] {
    case Assignment(a, b) => q"$pack.Assignment($a, $b)"
  }

  implicit val valueLiftable: Liftable[Value] = Liftable[Value] {
    case NullValue   => q"$pack.NullValue"
    case Constant(a) => q"$pack.Constant(${Literal(c.universe.Constant(a))})"
    case Tuple(a)    => q"$pack.Tuple($a)"
  }
  implicit val identLiftable: Liftable[Ident] = Liftable[Ident] {
    case Ident(a) => q"$pack.Ident($a)"
  }
}