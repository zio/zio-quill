package io.getquill.util

import java.io.PrintStream

import io.getquill.AstPrinter
import io.getquill.AstPrinter.Implicits._
import io.getquill.util.Messages.TraceType
import io.getquill.util.IndentUtil._

import scala.collection.mutable
import scala.util.matching.Regex

case class TraceConfig(enabledTraces: List[TraceType])
object TraceConfig {
  val Empty = new TraceConfig(List())
}

class Interpolator(
  traceType: TraceType,
  traceConfig: TraceConfig,
  defaultIndent: Int = 0,
  color: Boolean = Messages.traceColors,
  qprint: AstPrinter = Messages.qprint,
  out: PrintStream = System.out,
  globalTracesEnabled: (TraceType) => Boolean = Messages.tracesEnabled(_)
) {
  implicit class InterpolatorExt(sc: StringContext) {
    def trace(elements: Any*) = new Traceable(sc, elements)
  }

  def tracesEnabled(traceType: TraceType) =
    traceConfig.enabledTraces.contains(traceType) || globalTracesEnabled(traceType)

  class Traceable(sc: StringContext, elementsSeq: Seq[Any]) {

    private val elementPrefix = "|  "

    private sealed trait PrintElement
    private case class Str(str: String, first: Boolean) extends PrintElement
    private case class Elem(value: String)              extends PrintElement
    private case class Simple(value: String)            extends PrintElement
    private case object Separator                       extends PrintElement

    private def generateStringForCommand(value: Any, indent: Int) = {
      val objectString = qprint(value).string(color)
      val oneLine      = objectString.fitsOnOneLine
      oneLine match {
        case true => s"${indent.prefix}> ${objectString}"
        case false =>
          s"${indent.prefix}>\n${objectString.multiline(indent, elementPrefix)}"
      }
    }

    private def readFirst(first: String) =
      new Regex("%([0-9]+)(.*)").findFirstMatchIn(first) match {
        case Some(matches) =>
          (matches.group(2).trim, Some(matches.group(1).toInt))
        case None => (first, None)
      }

    sealed trait Splice { def value: String }
    object Splice {
      case class Simple(value: String) extends Splice // Simple splice into the string, don't indent etc...
      case class Show(value: String)   extends Splice // Indent, colorize the element etc...
    }

    private def readBuffers() = {
      def orZero(i: Int): Int = if (i < 0) 0 else i

      val parts = sc.parts.iterator.toList
      val elements = elementsSeq.toList.map { elem =>
        if (elem.isInstanceOf[String]) Splice.Simple(elem.asInstanceOf[String])
        else Splice.Show(qprint(elem).string(color))
      }

      val (firstStr, explicitIndent) = readFirst(parts.head)
      val indent =
        explicitIndent match {
          case Some(value) => value
          case None => {
            // A trick to make nested calls of andReturn indent further out which makes andReturn MUCH more usable.
            // Just count the number of times it has occurred on the thread stack.
            val returnInvocationCount = Thread
              .currentThread()
              .getStackTrace
              .toList
              .count(e => e.getMethodName == "andReturn")
            defaultIndent + orZero(returnInvocationCount - 1) * 2
          }
        }

      val partsIter = parts.iterator
      partsIter.next() // already took care of the 1st element
      val elementsIter = elements.iterator

      val sb = new mutable.ArrayBuffer[PrintElement]()
      sb.append(Str(firstStr.trim, true))

      while (elementsIter.hasNext) {
        val nextElem = elementsIter.next()
        nextElem match {
          case Splice.Simple(v) =>
            sb.append(Simple(v))
            val nextPart = partsIter.next().trim
            sb.append(Simple(nextPart))
          case Splice.Show(v) =>
            sb.append(Separator)
            sb.append(Elem(v))
            val nextPart = partsIter.next().trim
            sb.append(Separator)
            sb.append(Str(nextPart, false))
        }
      }

      (sb.toList, indent)
    }

    def generateString() = {
      val (elementsRaw, indent) = readBuffers()

      val elements = elementsRaw.filter {
        case Str(value, _) => value.trim != ""
        case Elem(value)   => value.trim != ""
        case _             => true
      }

      val oneLine = elements.forall {
        case Elem(value)   => value.fitsOnOneLine
        case Str(value, _) => value.fitsOnOneLine
        case _             => true
      }
      val output =
        elements.map {
          case Simple(value)                  => value
          case Str(value, true) if (oneLine)  => indent.prefix + value
          case Str(value, false) if (oneLine) => value
          case Elem(value) if (oneLine)       => value
          case Separator if (oneLine)         => " "
          case Str(value, true)               => value.multiline(indent, "")
          case Str(value, false)              => value.multiline(indent, "|")
          case Elem(value)                    => value.multiline(indent, "|  ")
          case Separator                      => "\n"
        }

      (output.mkString, indent)
    }

    private def logIfEnabled[T]() =
      if (tracesEnabled(traceType))
        Some(generateString())
      else
        None

    def andLog(): Unit =
      logIfEnabled().foreach(value => out.println(value._1))

    def andContinue[T](command: => T) = {
      logIfEnabled().foreach(value => out.println(value._1))
      command
    }

    def andReturn[T](command: => T) =
      logIfEnabled() match {
        case Some((output, indent)) =>
          // do the initial log
          out.println(output)
          // evaluate the command, this will activate any traces that were inside of it
          val result = command
          out.println(generateStringForCommand(result, indent))

          result
        case None =>
          command
      }

    def andReturnIf[T](command: => T)(showIf: T => Boolean) =
      logIfEnabled() match {
        case Some((output, indent)) =>
          // Even though we usually want to evaluate the command after the initial log was done
          // (so that future logs are nested under this one after the intro text but not
          // before the return) but we can't do that in this case because the switch indicating
          // whether to output anything or not is dependant on the return value.
          val result = command

          if (showIf(result))
            out.println(output)

          if (showIf(result))
            out.println(generateStringForCommand(result, indent))

          result
        case None =>
          command
      }
  }
}
