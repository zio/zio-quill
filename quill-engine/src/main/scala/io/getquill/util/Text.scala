package io.getquill.util

import io.getquill.ast.{Ast, Ident, Pos}
import io.getquill.context.sql.idiom.Error

// Intentionally put all comments in 1st line. Want this to be a place
// where example code can be put
// format: off

object Text {

implicit class StringExt(str: String) {
  def trimLeft = str.dropWhile(_.isWhitespace)
}

implicit class SeqExt[T](seq: Seq[T]) {
  def sortByVariant[R](pf: PartialFunction[T, R])(implicit ord: Ordering[R]) = {
    seq.filter(v => pf.isDefinedAt(v)).sortBy(v => pf(v))
  }
}


def JoinSynthesisError(errors: List[Error]) =
  errors match {
    case List(Error(List(id), ast)) => joinSynthesisErrorSingle(id, ast)
    case _ => joinSynthesisErrorMulti(errors)
  }


// make a special printout case for a single error since this is what happens 95% of the time
private def joinSynthesisErrorSingle(id: Ident, ast: Ast) =
s"""
When synthesizing Joins, Quill found a variable that could not be traced back to its origin: ${id.name}
originally at: ${id.pos}

with the following faulty expression:
${ast}

${joinSynthesisExplanation}
""".trimLeft


private def joinSynthesisErrorMulti(errors: List[Error]) = {
val allVars  = errors.flatMap(_.free).distinct
val firstVar = errors.headOption.flatMap(_.free.headOption).getOrElse("someVar")

def printError(error: Error) =
s"""
====== Faulty Expression ======
${error.ast}
Variables:
${error.free.map(id => s"${id.name} - ${id.pos.print}").mkString("\n")}
""".trimLeft

s"""
When synthesizing Joins, Quill found some variables that could not be traced back to their
origin: ${allVars.map(_.name)}.

${joinSynthesisExplanation}
""".trimLeft +
errors.map(printError(_)).mkString(",\n")
}


private lazy val joinSynthesisExplanation =
s"""
Typically this happens when there are some flatMapped
clauses that are missing data once they are flattened.
Sometimes this is the result of a internal error in Quill. If that is the case, please
reach out on our discord channel https://discord.gg/2ccFBr4 and/or file an issue
on https://github.com/zio/zio-quill.
""".trimLeft

// ========================================= FreeVariablesExitError =========================================

def FreeVariablesExitError(freeVars: Seq[Ident], showPos: Boolean = true): String =
  if (freeVars.size == 1)
    freeVariablesSingle(freeVars.head, showPos)
  else
    freeVariablesMulti(freeVars)

// Most of the time there are free varaibles it's just one so make a specific message optimizing for that
// if we're in a compile-time flow we don't need to show the position because it will be conveyed directly to the compiler.
private def freeVariablesSingle(freeVar: Ident, showPos: Boolean) =
s"""
Found the following variable: ${freeVar} that originates outside of a `quote {...}` or `run {...}` block.
${if (showPos) s"Here: ${freeVar.pos.print}\n" else ""}
${freeVariablesExplanation(freeVar.name)}
""".trimLeft

private def freeVariablesMulti(freeVarsUnordered: Seq[Ident]) = {
  val knowPosVars =
    freeVarsUnordered.sortByVariant {
      case value @ Ident.WithPos(_, Pos.Real(file, line, col, _, _)) => (file, line, col)
    }
  val unknownPosVars =
    freeVarsUnordered.sortByVariant {
      case Ident.WithPos(name, Pos.Synthetic) => name
    }
  val allVars = knowPosVars ++ unknownPosVars
  val free = allVars.map(_.name)
  val firstVar = free.headOption.getOrElse("x")
  val locations =
    if (knowPosVars.nonEmpty) {
      knowPosVars.map(v => s"  ${v.name} - ${v.pos.print}").mkString("\n") + "\n"
    } else
    ""

s"""
Found the following variables: ${free} that seem to originate outside of a `quote {...}` or `run {...}` block.
${locations}
${freeVariablesExplanation(firstVar)}
""".trimLeft


}

private def freeVariablesExplanation(varExample: String) =
s"""
Quotes and run blocks cannot use values outside their scope directly (with the exception of inline expressions in Scala 3).
In order to use runtime values in a quotation, you need to lift them, so instead
of this `$varExample` do this: `lift($varExample)`.
Here is a more complete example:
Instead of this: `def byName(n: String) = quote(query[Person].filter(_.name == n))`
        Do this: `def byName(n: String) = quote(query[Person].filter(_.name == lift(n)))`
}
""".trimLeft


}
// format: on
