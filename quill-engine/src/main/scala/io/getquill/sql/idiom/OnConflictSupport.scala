package io.getquill.context.sql.idiom

import io.getquill.ast._
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Token
import io.getquill.NamingStrategy
import io.getquill.IdiomContext
import io.getquill.util.Messages.fail

trait OnConflictSupport {
  self: SqlIdiom =>

  implicit def conflictTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[OnConflict] = {
    val entityAlias = "t"

    val customEntityTokenizer = Tokenizer[Entity] { case Entity.Opinionated(name, _, _, renameable) =>
      stmt"INTO ${renameable.fixedOr(name.token)(strategy.table(name).token)} AS ${entityAlias.token}"
    }

    val customAstTokenizer =
      Tokenizer.withFallback[Ast](self.astTokenizer(_, strategy, idiomContext)) {
        case Property(_: OnConflict.Excluded, value) => stmt"EXCLUDED.${value.token}"

        // At first glance it might be hard to understand why this is doing `case OnConflict.Existing(a) => stmt"${entityAlias}"`
        // but consider that this is a situation where multiple aliases are used in multiple update clauses e.g. the `tt` in the below example
        // wouldn't even exist as a variable because in the query produced (also below) it would not even exist
        // i.e. since the table will be aliased as the first `existing table` variable i.e. `t`.
        // The second one `tt` wouldn't even exist.
        // ins.onConflictUpdate(_.i, _.s)(
        //    (t, e) => t.l -> foo(t.l, e.l), (tt, ee) => tt.l -> bar(tt.l, ee.l)
        //  )
        //                                                                                                                            This doesn't exist!!
        //                                                                                                                                    v
        // > INSERT INTO TestEntity AS t (s,i,l,o,b) VALUES (?, ?, ?, ?, ?) ON CONFLICT (i,s) DO UPDATE SET l = foo(t.l, EXCLUDED.l), l = bar(tt.l, EXCLUDED.l)
        //
        // So instead of the situation we use the original alias of the table as it was computed by Quill i.e. `t`.
        // See the "cols target - update + infix" example for more detail
        case _: OnConflict.Excluded => stmt"EXCLUDED"
        case _: OnConflict.Existing => stmt"${entityAlias.token}"
        case a: Action =>
          self.actionTokenizer(customEntityTokenizer)(actionAstTokenizer, strategy, idiomContext).token(a)
      }

    import OnConflict._

    def doUpdateStmt(i: Token, t: Token, u: Update) = {
      val assignments = u.assignments
        .map(a => stmt"${actionAstTokenizer.token(a.property)} = ${scopedTokenizer(a.value)(customAstTokenizer)}")
        .mkStmt()(statementTokenizer)

      stmt"$i ON CONFLICT $t DO UPDATE SET $assignments"
    }

    def doNothingStmt(i: Ast, t: Token) = stmt"${i.token} ON CONFLICT $t DO NOTHING"

    implicit val conflictTargetPropsTokenizer: Tokenizer[Properties] =
      Tokenizer[Properties] { case OnConflict.Properties(props) =>
        stmt"(${props.map(n => n.renameable.fixedOr(n.name)(strategy.column(n.name))).mkStmt(",")})"
      }

    def tokenizer(implicit astTokenizer: Tokenizer[Ast]): Tokenizer[OnConflict] =
      Tokenizer[OnConflict] {
        case OnConflict(_, NoTarget, _: Update)      => fail("'DO UPDATE' statement requires explicit conflict target")
        case OnConflict(i, p: Properties, u: Update) => doUpdateStmt(i.token, p.token, u)

        case OnConflict(i, NoTarget, Ignore)      => stmt"${astTokenizer.token(i)} ON CONFLICT DO NOTHING"
        case OnConflict(i, p: Properties, Ignore) => doNothingStmt(i, p.token)
      }

    tokenizer(customAstTokenizer)
  }
}
