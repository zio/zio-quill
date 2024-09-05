package io.getquill

import io.getquill.ast.Ast

/**
 * Defines the primary interface by which information in Quill is composed. This
 * includes not only queries but all code fragments. A quotation can be a simple
 * value: {{ val pi = quote(3.14159) }} And be used within another quotation: {{
 * case class Circle(radius: Float)
 *
 * val areas = quote { query[Circle].map(c => pi * c.radius * c.radius) } }}
 * Quotations can also contain high-order functions and inline values: {{ val
 * area = quote { (c: Circle) => { val r2 = c.radius * c.radius pi * r2 } } val
 * areas = quote { query[Circle].map(c => area(c)) } }}
 *
 * Note that this class must not be in quill-engine since it cannot be shared
 * with ProtoQuill and which has a different implementation of Quoted.
 * @see
 *   <a
 *   href="https://youtrack.jetbrains.com/issue/SCL-20078/Scala-3.1.1-fails-to-compile-no-method-options#focus=Comments-27-6048464.0-0">Scala
 *   3.1.1 fails to compile / no method options</a> for more info.
 */
trait Quoted[+T] {
  def ast: Ast
  override def toString: String = ast.toString
}
