package io.getquill.quat

import io.getquill.quat.FindBranches.BranchQuat.PathElement
import io.getquill.quat.FindBranches.Result
import io.getquill.quat.FindBranches.Result.ProductWithBranches

import scala.annotation.tailrec

object VerifyNoBranches {
  private implicit final class DotsExt(private val list: List[String]) extends AnyVal {
    def dots: String = list.mkString(".")
  }

  private implicit final class StringExt(private val str: String) extends AnyVal {
    def appendIfNotEmpty(value: String): String =
      str.trim match {
        case ""    => ""
        case other => other + value
      }
    def isNonEmpty: Boolean = str.trim != ""
  }

  case class BranchFound(
    outerClass: Option[String],
    pathToInnerField: String,
    innerField: String,
    possibleInnerPaths: List[String]
  )
  object BranchFound {
    def constructFrom(result: FindBranches.Result): List[BranchFound] = {
      def recurse(result: FindBranches.Result, classesToHere: List[String]): List[BranchFound] =
        result match {
          case Result.SingleBranch(path, innermostField) =>
            val pathToInnerField = classesToHere ++ path.map(_.fieldClassName)
            val innerField       = innermostField
            // CCOuter(foo: CCFoo(bar: CCBar(baz: V))) is Branch(foo->CCFoo,bar->CCBar, baz)
            val shiftedBranchPaths = {
              val pathToHere = classesToHere.dots
              // from foo->CCFoo,bar->CCBar
              // take foo,bar
              val shiftedFields = path.map(_.field)
              // (CCFoo,CCBar).dropRight(1) =>
              //   Then tack "CCOuter." to everything
              //   Then prepend with "CCOuter" since that's the root-level class
              //   So then we have List(CCOuter, "CCOuter." + CCFoo)
              val fullPathClasses =
                pathToHere +: path.map(_.fieldClassName).dropRight(1).map(pathToHere.appendIfNotEmpty(".") + _)
              // recompose into foo->CCOuter,bar->CCFoo
              (shiftedFields zip fullPathClasses).collect {
                // This should become List(CCOuter.foo, CCOuter.CCFoo.bar)
                case (field, cls) if (field.isNonEmpty && cls.isNonEmpty) => cls + "." + field
              }
            }
            List(BranchFound(classesToHere.headOption, pathToInnerField.dots, innerField, shiftedBranchPaths))
          case ProductWithBranches(name, children) =>
            children.flatMap(child => recurse(child, classesToHere :+ name))
        }

      val foundBranches = recurse(result, List.empty)
      // In some situations e.g. CCOuter(a: CCFoo(bar: CCBar(baz), b: CCFoo(bar: CCBar(baz)) we will have two messages:
      // field 'baz' will be used instead of a in Outer.a
      // and:
      // field 'baz' will be used instead of a in Outer.b
      // Should merge these together
      foundBranches
        .groupBy(b => (b.outerClass, b.innerField, b.pathToInnerField))
        .map { case ((outerClass, innerField, pathToInnerField), branches) =>
          BranchFound(
            outerClass,
            pathToInnerField,
            innerField,
            branches.flatMap(_.possibleInnerPaths)
          )
        }
        .toList
    }
  }

  // TODO What about a a top-level branch, what's the text for that?
  case class BranchFoundMessage(msg: String)
  object BranchFoundMessage {
    def makeFrom(found: BranchFound): BranchFoundMessage = {
      val link = "https://getquill.io/#extending-quill-custom-encoding"
      // The field 'value' in Person.Name.First will be use in the query[Person] instead of Person.name or Person.Name.first.
      // Are you sure this is the intended behavior? Perhaps you meant to write an encoder/decoder for Person.Name.First?
      // See the section on Mapped Encodings in the quill documentation here: <> for the simplest way to do that.
      val msg =
        s"The field '${found.innerField}' in the object ${found.pathToInnerField} will be used in the query${found.outerClass
            .map(r => s"[$r]")
            .getOrElse("")} " +
          s"instead of the field ${found.possibleInnerPaths.mkString(" or ")}." +
          s"\nAre you sure this is the intended behavior? " +
          s"Perhaps you meant to write an encoder/decoder for ${found.pathToInnerField}?" +
          s"\nSee the section on Mapped Encodings in the quill " +
          s"documentation here: $link for the simplest way to do that."
      BranchFoundMessage(msg)
    }
  }

  case class Output(messages: List[BranchFoundMessage])
  def in(quat: Quat): Output = {
    val foundBranchResults = FindBranches.in(quat)
    val foundBranches      = foundBranchResults.map(BranchFound.constructFrom(_)).getOrElse(List.empty)
    Output(foundBranches.map(BranchFoundMessage.makeFrom))
  }
}

private[getquill] object FindBranches {

  sealed trait Result
  object Result {
    case class SingleBranch private[quat] (path: List[PathElement], innermostField: String) extends Result
    case class ProductWithBranches(name: String, children: List[Result])                    extends Result
  }

  def in(quat: Quat): Option[Result] = recurseFind("root", quat)

  private def recurseFind(currentPropName: String, quat: Quat): Option[Result] =
    quat match {
      // CCParent(foo:CCFoo(bar: CCBar(baz: Value), other:Value) - say we are in CCParent and recursing on `foo` which becomes:
      // -> Result.SingleBranch(Branch(Path(root->CCFoo,bar->CCBar),baz)) - when we parse it, it became this
      // -> Result.SingleBranch(Branch(Path(foo->CCFoo,bar->CCBar),baz)) - from the upper level we know the property name is foo, so swap that in
      case BranchQuat(branch) =>
        Some(Result.SingleBranch(branch.pathWithRootField(currentPropName), branch.innermostField))
      // if it is not a product then it has to be a leaf at this point, return nothing
      case _ if (!quat.isProduct) =>
        None
      // if it is a product, check if there are any branches or products-with-branches inside
      case p: Quat.Product =>
        val children =
          p.fields.collect { case (name, quat: Quat.Product) =>
            recurseFind(name, quat)
          }.toList.collect { case Some(v) =>
            v
          }
        Some(ProductWithBranches(p.name, children))
    }

  object BranchQuat {
    def unapply(quat: Quat): Option[Branch] =
      quat match {
        // Need to match outermost one and know we're starting a branch for the recursion chain to work correctly
        // (see the assumption inside)
        case SingletonProduct(productName, (childField, childQuat)) =>
          recurse(childField, childQuat, AccumPath.make(productName))
        case _ =>
          None
      }

    case class AccumPath private[quat] (first: RootElement, rest: List[PathElement]) {
      def :+(elem: PathElement): AccumPath = this.copy(rest = rest :+ elem)
    }
    object AccumPath {
      def make(firstClassName: String): AccumPath = new AccumPath(RootElement(firstClassName), List.empty)
    }
    @tailrec
    def recurse(thisField: String, thisQuat: Quat, path: AccumPath): Option[Branch] =
      thisQuat match {
        // If it's a singleton product it might be a branch all the way through
        case SingletonProduct(productName, (childField, childQuat)) =>
          recurse(childField, childQuat, path :+ PathElement(thisField, productName))
        // If it's a product that's not singleton we know it's not a singleton branch
        case _: Quat.Product => None
        // (assuming we are already in a brach of at least 1-depth) if we ran into a Quat Value return the path to it
        case _ => Some(Branch(path.first, path.rest, thisField))
      }

    object SingletonProduct {
      def unapply(quat: Quat): Option[(String, (String, Quat))] =
        quat match {
          case p: Quat.Product if (p.fields.size == 1) => Some((p.name, p.fields.head))
          case _                                       => None
        }
    }

    case class RootElement(fieldClassName: String)
    case class PathElement(field: String, fieldClassName: String)

    case class Branch(first: RootElement, tailPath: List[PathElement], innermostField: String) {
      // field name originally given to a branch will always be "root" but this allows a higher level mechanism to set it since
      // the higher level mechanism should know the parent-product of the whole branch
      def pathWithRootField(fieldName: String): List[PathElement] =
        PathElement(fieldName, first.fieldClassName) +: tailPath
    }
  }
}
