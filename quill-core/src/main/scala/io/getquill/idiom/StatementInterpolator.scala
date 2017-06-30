package io.getquill.idiom

import io.getquill.ast._
import io.getquill.util.Interleave
import io.getquill.util.Messages._

object StatementInterpolator {

  trait Tokenizer[T] {
    def token(v: T): Token
  }

  object Tokenizer {
    def apply[T](f: T => Token) = new Tokenizer[T] {
      def token(v: T) = f(v)
    }
  }

  implicit class TokenImplicit[T](v: T)(implicit tokenizer: Tokenizer[T]) {
    def token = tokenizer.token(v)
  }

  implicit def stringTokenizer: Tokenizer[String] =
    Tokenizer[String] {
      case string => StringToken(string)
    }

  implicit def liftTokenizer: Tokenizer[Lift] =
    Tokenizer[Lift] {
      case lift: ScalarLift => ScalarLiftToken(lift)
      case lift             => fail(s"Can't tokenize a non-scalar lifting. ${lift.name}")
    }

  implicit def tokenTokenizer: Tokenizer[Token] = Tokenizer[Token](identity)
  implicit def statementTokenizer: Tokenizer[Statement] = Tokenizer[Statement](identity)
  implicit def stringTokenTokenizer: Tokenizer[StringToken] = Tokenizer[StringToken](identity)
  implicit def liftingTokenTokenizer: Tokenizer[ScalarLiftToken] = Tokenizer[ScalarLiftToken](identity)

  implicit class TokenList[T](list: List[T])(implicit tokenize: Tokenizer[T]) {
    def mkStmt(sep: String = ", ")(implicit tokenize: Tokenizer[T]) = {
      val l1 = list.map(_.token)
      val l2 = List.fill(l1.size - 1)(StringToken(sep))
      Statement(Interleave(l1, l2))
    }
  }

  implicit def listTokenizer[T](implicit tokenize: Tokenizer[T]): Tokenizer[List[T]] =
    Tokenizer[List[T]] {
      case list => list.mkStmt()
    }

  implicit class Impl(sc: StringContext) {

    private def flatten(tokens: List[Token]): List[Token] = {

      def unestStatements(tokens: List[Token]): List[Token] =
        tokens.foldLeft(List.empty[Token]) {
          case (acc, Statement(tokens)) =>
            acc ++ unestStatements(tokens)
          case (acc, token) =>
            acc :+ token
        }

      def mergeStringTokens(tokens: List[Token]) =
        (tokens.foldLeft((List[Token](), StringToken(""): Token)) {
          case ((acc, a: StringToken), b: StringToken) =>
            val merged = StringToken(s"${a.string}${b.string}")
            (acc, merged)
          case ((acc, a), b) =>
            (acc :+ a, b)
        }) match {
          case (acc, b) => acc :+ b
        }

      def removeEmptyStringTokens(tokens: List[Token]) =
        tokens.filterNot(_ == StringToken(""))

      (unestStatements _)
        .andThen(mergeStringTokens _)
        .andThen(removeEmptyStringTokens _)
        .apply(tokens)
    }

    def stmt(args: Token*): Statement = {
      sc.checkLengths(args)
      val partsIterator = sc.parts.iterator
      val argsIterator = args.iterator
      val bldr = List.newBuilder[Token]
      bldr += StringToken(partsIterator.next())
      while (argsIterator.hasNext) {
        bldr += argsIterator.next
        bldr += StringToken(partsIterator.next())
      }
      val tokens = flatten(bldr.result)
      Statement(tokens)
    }
  }
}