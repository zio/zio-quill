package io.getquill.codegen.util
import java.io.Closeable

import io.getquill.codegen.integration.CodegenTestCases
import io.getquill.context.jdbc.JdbcContext
import javax.sql.DataSource

trait WithContext[Prefix <: ConfigPrefix, CTest <: CodegenTestCases] extends SchemaMaker {
  type QuillContext <: JdbcContext[_, _]

  def ctest: CTest
  def dbPrefix: Prefix

  protected def makeContext(ds: DataSource with Closeable): QuillContext
  def run(testCode: QuillContext => Any): Unit = {
    withContext(ctest.schemaMakerCoordinates(dbPrefix))(testCode)
    ()
  }
}

abstract class WithContextBase[Prefix <: ConfigPrefix, CTest <: CodegenTestCases](val dbPrefix: Prefix, val ctest: CTest) extends WithContext[Prefix, CTest]

// Cannot include WithOracleContext here since it is not being compiled in all builds
object WithContext extends WithContextAux
  with WithH2Context
  with WithMysqlContext
  with WithPostgresContext
  with WithSqliteContext
  with WithSqlServerContext

trait WithContextAux {
  type Aux[Prefix <: ConfigPrefix, CTest <: CodegenTestCases, Ret] = WithContext[Prefix, CTest] { type QuillContext = Ret }
  def apply[Prefix <: ConfigPrefix, CTest <: CodegenTestCases](implicit cft: WithContext[Prefix, CTest]): Aux[Prefix, CTest, cft.QuillContext] = cft
}
