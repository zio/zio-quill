package io.getquill.source.cassandra.cluster

import com.typesafe.config.Config

object ClusterSession {

  def apply(cfg: Config) =
    ClusterBuilder(cfg.getConfig("session")).build
      .connect(cfg.getString("keyspace"))
}
