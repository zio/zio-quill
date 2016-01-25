package io.getquilll.source.play

import java.io.Closeable
import javax.sql.DataSource

import io.getquill.naming.NamingStrategy
import io.getquill.source.jdbc.JdbcSource
import io.getquill.source.sql.idiom.SqlIdiom

/**
  * Source to be used with the Play frameworks. It connects to Play's Hikari data source and reuses it.
  */
class PlaySource[ D <: SqlIdiom, N <: NamingStrategy ] extends JdbcSource[ D, N ] {

  override def close: Unit = {} // do not close the source, Play will do it

  override protected def createDataSource: DataSource with Closeable =
  // reuse data source managed by Play
    play.api.db.DB.getDataSource( )( play.api.Play.current ).asInstanceOf[ DataSource with Closeable ]
}
