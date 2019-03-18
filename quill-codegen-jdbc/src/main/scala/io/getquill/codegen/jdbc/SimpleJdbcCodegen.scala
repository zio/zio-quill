package io.getquill.codegen.jdbc

import com.typesafe.config.Config
import io.getquill.JdbcContextConfig
import io.getquill.codegen.jdbc.gen.JdbcGeneratorBase
import io.getquill.codegen.jdbc.model.JdbcTypes.JdbcConnectionMaker
import io.getquill.codegen.model.{ LiteralNames, NameParser }
import io.getquill.util.LoadConfig
import javax.sql.DataSource

/**
 * The purpose of the simple code generator is to generate simple case classes representing tables
 * in a database. Create one or multiple <code>CodeGeneratorConfig</code> objects
 * and call the <code>.writeFiles</code> or <code>.writeStrings</code> methods
 * on the code generator and the reset happens automatically.
 */
class SimpleJdbcCodegen(
  override val connectionMakers: Seq[JdbcConnectionMaker],
  override val packagePrefix:    String                   = ""
) extends JdbcGeneratorBase(connectionMakers, packagePrefix) {

  def this(connectionMaker: JdbcConnectionMaker, packagePrefix: String) = this(Seq(connectionMaker), packagePrefix)

  def this(dataSource: DataSource, packagePrefix: String) =
    this(Seq(() => { dataSource.getConnection }), packagePrefix)
  def this(config: JdbcContextConfig, packagePrefix: String) =
    this(config.dataSource, packagePrefix)
  def this(config: Config, packagePrefix: String) = this(JdbcContextConfig(config), packagePrefix)
  def this(configPrefix: String, packagePrefix: String) = this(LoadConfig(configPrefix), packagePrefix)

  override def nameParser: NameParser = LiteralNames
}
