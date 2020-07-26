package io.getquill.quat

import io.getquill.quotation.QuatException

import scala.collection.mutable

object LinkedHashMapOps {
  implicit class LinkedHashMapExt[K, V](m1: mutable.LinkedHashMap[K, V]) {
    def zipWith[R](m2: mutable.LinkedHashMap[K, V])(f: PartialFunction[(K, V, Option[V]), R]) =
      LinkedHashMapOps.zipWith(m1, m2, f)
  }

  def zipWith[K, V, R](m1: mutable.LinkedHashMap[K, V], m2: mutable.LinkedHashMap[K, V], f: PartialFunction[(K, V, Option[V]), R]) = {
    m1.iterator.map(r => (r._1, r._2, m2.get(r._1))).collect(f)
  }
}

// This represents a simplified Sql-Type. Since it applies to all dialects, it is called
// Quill-Application-Type hence Quat.
sealed trait Quat {
  def applyRenames: Quat = this
  def withRenames(renames: List[(String, String)]): Quat

  def renames: List[(String, String)] = List()

  /** Either convert to a Product or make the Quat into an error if it is anything else. */
  def probit =
    this match {
      case p: Quat.Product => p
      case other           => QuatException(s"Was expecting SQL-level type must be a Product but found `${other}`")
    }

  def leastUpperType(other: Quat): Option[Quat] = {
    (this, other) match {
      case (Quat.Generic, other)                   => Some(other)
      case (Quat.Null, other)                      => Some(other)
      case (other, Quat.Generic)                   => Some(other)
      case (other, Quat.Null)                      => Some(other)
      case (Quat.Value, Quat.Value)                => Some(Quat.Value)
      case (me: Quat.Product, other: Quat.Product) => me.leastUpperTypeProduct(other)
      case (_, _)                                  => None
    }
  }

  override def toString: String = shortString

  def shortString: String = this match {
    case Quat.Product(fields) => s"CC(${
      fields.map {
        case (k, v) => k + (v match {
          case Quat.Value => ""
          case other      => ":" + other.shortString
        })
      }.mkString(",")
    })${
      (this.renames match {
        case Nil   => ""
        case other => s"[${other.map { case (k, v) => k + "->" + v }.mkString(",")}]"
      })
    }"
    case Quat.Generic => "<G>"
    case Quat.Value   => "V"
    case Quat.Null    => "N"
  }

  /** What was the value of a given property before it was renamed (i.e. looks up the value of the Renames hash) */
  def beforeRenamed(path: String): Option[String] = (this, path) match {
    case (cc: Quat.Product, fieldName) =>
      // NOTE This is a linear lookup. To improve efficiency store a map going back from rename to the initial property,
      // if we did that however, we would need to make sure to warn a user of two things are renamed to the same property however,
      // that kind of warning should probably exist already
      renames.find(_._2 == fieldName).headOption.map(_._2)
    case (other, fieldName) =>
      QuatException(s"The post-rename field '${fieldName}' does not exist in an SQL-level type ${other}")
  }

  def lookup(path: String): Quat = (this, path) match {
    case (cc @ Quat.Product(fields), fieldName) =>
      // TODO Change to Get
      fields.find(_._1 == fieldName).headOption.map(_._2).getOrElse(QuatException(s"The field ${fieldName} does not exist in the SQL-level ${cc}"))
    case (other, fieldName) =>
      QuatException(s"The field '${fieldName}' does not exist in an SQL-level type ${other}")
  }
  def lookup(list: List[String]): Quat =
    list match {
      case head :: tail => this.lookup(head).lookup(tail)
      case Nil          => this
    }
}

object Quat {
  import LinkedHashMapOps._

  object BottomType {
    def unapply(quat: Quat) =
      quat match {
        case Quat.Null | Quat.Generic => true
        case _                        => false
      }
  }

  object TupleIndex {
    def is(s: String): Boolean = unapply(s).isDefined
    def unapply(s: String): Option[Int] =
      if (s.matches("_[0-9]*"))
        Some(s.drop(1).toInt - 1)
      else
        None
  }

  case class Product(fields: mutable.LinkedHashMap[String, Quat]) extends Quat {

    def this(list: Iterator[(String, Quat)]) = this((mutable.LinkedHashMap[String, Quat]() ++ list): mutable.LinkedHashMap[String, Quat])

    def withRenamesFrom(other: Quat): Quat = {
      other match {
        case otherProduct: Quat.Product =>
          val newFields =
            fields.map {
              case (key, value) =>
                otherProduct.fields.find(_._1 == key).map { case (ok, ov) => (key, ov, value) }.toRight((key, value))
            }.map {
              // If the other Quat.Product does not have this field, don't rename it, just return it as is
              case Left((key, value))                       => (key, value)
              // If the other Quat.Product has this field and the value of that field is also a product, recurse into it
              case Right((key, from: Product, to: Product)) => (key, to.withRenamesFrom(from))
              // If the value of the other field is not a product, just return the original field/value
              case Right((key, _, to))                      => (key, to)
            }
          // Pass in the local renames from the other quat product
          Quat.Product(newFields).withRenames(otherProduct.renames)

        case _ => this
      }
    }

    def leastUpperTypeProduct(other: Quat.Product): Option[Quat.Product] = {
      val newFieldsIter =
        fields.zipWith(other.fields) {
          case (key, thisQuat, Some(otherQuat)) => (key, thisQuat.leastUpperType(otherQuat))
        }.collect {
          case (key, Some(value)) => (key, value)
        }
      val newFields = mutable.LinkedHashMap(newFieldsIter.toList: _*)
      // Note, some extra renames from properties that don't exist could make it here.
      // Need to make sure to ignore extra ones when they are actually applied.
      Some(Quat.Product(newFields).withRenames(renames))
    }

    def withRenames(renames: List[(String, String)]) =
      Product.WithRenames(fields, renames)

    /**
     * Rename the properties based on the renames list. Keep this list
     * around since it is used in sql sub-query expansion to determine whether
     * the property is fixed or not (i.e. whether the column naming strategy should
     * be applied to it).
     */
    override def applyRenames: Quat.Product = {
      val newFields = fields.map {
        case (f, q) =>
          // Rename properties of this quat and rename it's children recursively
          val newKey = renames.find(_._1 == f).map(_._2).getOrElse(f)
          val newValue = q.applyRenames
          (newKey, newValue)
      }
      Product.WithRenames(newFields, renames)
    }
  }
  def LeafProduct(list: String*) = Quat.Product(list.map(e => (e, Quat.Value)))
  def LeafTuple(numElems: Int) = Quat.Tuple((1 to numElems).map(_ => Quat.Value))

  object Product {
    def empty = new Quat.Product(mutable.LinkedHashMap[String, Quat]())
    def apply(fields: (String, Quat)*): Quat.Product = apply(fields.iterator)
    def apply(fields: Iterable[(String, Quat)]): Quat.Product = new Quat.Product(fields.iterator)
    def apply(fields: Iterator[(String, Quat)]): Quat.Product = new Quat.Product(fields)
    def unapply(p: Quat.Product): Some[mutable.LinkedHashMap[String, Quat]] = Some(p.fields)

    /**
     * Add staged renames to the Quat. Note that renames should
     * explicit NOT be counted as part of the Type indicated by the Quat
     * since it is typical to beta reduce a Quat without renames to a Quat with them
     * (see `PropagateRenames` for more detail)
     */
    object WithRenames {
      def apply(fields: mutable.LinkedHashMap[String, Quat], theRenames: List[(String, String)]) =
        new Product(fields) {
          override def renames: List[(String, String)] = theRenames
        }
      def unapply(p: Quat.Product) =
        Some((p.fields, p.renames))
    }
  }
  object Tuple {
    def apply(fields: Quat*): Quat.Product = apply(fields)
    def apply(fields: Iterable[Quat]): Quat.Product = new Quat.Product(fields.zipWithIndex.map { case (f, i) => (s"_${i + 1}", f) }.iterator)
  }
  case object Null extends Quat {
    override def withRenames(renames: List[(String, String)]) = this
  }
  case object Generic extends Quat {
    override def withRenames(renames: List[(String, String)]) = this
  }
  object Value extends Quat {

    /** Should not be able to rename properties on a value node, turns into a error of the array is not null */
    override def withRenames(renames: List[(String, String)]) =
      renames match {
        case Nil => this
        case _   => QuatException(s"Renames ${renames} cannot be applied to a value SQL-level type")
      }
  }
}
