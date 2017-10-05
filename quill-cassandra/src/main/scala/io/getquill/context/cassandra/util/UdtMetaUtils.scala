package io.getquill.context.cassandra.util

import io.getquill.util.Messages

object UdtMetaUtils {
  /**
   * Extracts udt name and keyspace from given path
   *
   * @param path udt path
   * @return (name, keyspace)
   */
  def parse(path: String): (String, Option[String]) = {
    val arr = path.split('.')
    if (arr.length == 1) arr(0) -> None
    else if (arr.length == 2) arr(1) -> Some(arr(0))
    else Messages.fail(s"Cannot parse udt path `$path`")
  }
}
