package io.getquill.codegen.util

object StringUtil {

  def indent(code: String): String = {
    val lines = code.split("\n")
    lines.tail.foldLeft(lines.head) { (out, line) =>
      out + '\n' +
        (if (line.isEmpty) line else "  " + line)
    }
  }

  implicit class StringExtensions(str: String) {
    def snakeToUpperCamel: String = str.split("_").map(_.toLowerCase).map(_.capitalize).mkString
    def snakeToLowerCamel         = str.split("_").map(_.toLowerCase).map(_.capitalize).mkString.uncapitalize
    def lowerCamelToSnake: String = str.split("(?=[A-Z])").mkString("_").toLowerCase
    def uncapitalize =
      new String(
        (str.toList match {
          case head :: tail => head.toLower :: tail
          case Nil          => Nil
        }).toArray
      )
    def unquote: String          = str.replaceFirst("^\"", "").replaceFirst("\"$", "")
    def trimFront: String        = str.dropWhile(_ == '\n')
    def notEmpty: Option[String] = if (str.trim == "") None else Some(str)
    def inSetNocase(seq: String*): Boolean =
      seq.map(_.toLowerCase).toSeq.contains(str.toLowerCase)
  }

  implicit class OptionStringExtensions(str: Option[String]) {
    def existsInSetNocase(seq: String*): Boolean =
      str.map(_.toLowerCase).exists(value => seq.map(_.toLowerCase).toSeq.contains(value))
  }
}
