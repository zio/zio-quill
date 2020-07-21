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
      case e: Quat.Error   => e
      case other           => Quat.Error(s"Was expecting SQL-level type must be a Product but found `${other}`")
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

  def leastUpperType2(other: Quat): Option[Quat] = {
    (this, other) match {
      case (Quat.Generic, other)                   => Some(other)
      case (Quat.Null, other)                      => Some(other)
      case (other, Quat.Generic)                   => Some(other)
      case (other, Quat.Null)                      => Some(other)
      case (Quat.Value, Quat.Value)                => Some(Quat.Value)
      case (me: Quat.Product, other: Quat.Product) => me.leastUpperTypeProduct2(other)
      case (_, _)                                  => None
    }
  }

  //  def reduceTo(other: Quat): Quat = {
  //    (this, other) match {
  //      case (Quat.Value, Quat.Value) => Quat.Value
  //      case (a: Quat.Product, b: Quat.Product) => a
  //      case (Quat.Null, Quat.Product(_)) => false // Null is most specific subtype so can't replace with a supertype of it
  //      case (Quat.Product(_), Quat.Null) => true
  //      case (Quat.Value, Quat.Null) => true
  //      case (_, _) => false
  //    }
  //  }

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
    case Quat.Error(msg, _) => s"Quat-Error(${msg.take(20)})"
    case Quat.Generic       => "<G>"
    case Quat.Value         => "V"
    case Quat.Null          => "N"
  }

  def lookup(path: String): Quat = (this, path) match {
    case (cc @ Quat.Product(fields), fieldName) =>
      fields.find(_._1 == fieldName).headOption.map(_._2).getOrElse(Quat.Error(s"The field ${fieldName} does not exist in the SQL-level ${cc}")) // TODO Quat does this not match in certain cases? Which ones?
    case (Quat.Value, fieldName) =>
      Quat.Error(s"The field '${fieldName}' does not exist in an SQL-level leaf-node") // TODO Quat is there a case where we're putting a property on a entity which is actually a value?
    case (Quat.Null, fieldName) =>
      Quat.Error(s"The field '${fieldName}' cannot be looked up from a SQL-level null node") // TODO Quat is there a case where we're putting a property on a entity which is actually a value?
    case (Quat.Generic, fieldName) =>
      Quat.Error(s"The field '${fieldName}' cannot be looked up from a SQL-level generic node") // TODO Quat is there a case where we're putting a property on a entity which is actually a value?
    case (Quat.Error(msg, _), _) => Quat.Error(s"${msg}. Also tried to lookup ${path} from node.")
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

  /** Represents a coproduct of a Product and Error */
  sealed trait Probity extends Quat {

    override def applyRenames: Quat.Probity =
      this match {
        case p: Product   => p.applyRenames
        case error: Error => error
      }

    /** Produce a product or throw an error. I.e. 'prove' the fact that the probity is a Product. */
    def prove =
      this match {
        case p: Quat.Product    => p
        case Quat.Error(msg, _) => throw new QuatException(s"Could not cast ProductOr SQL-Type to Product SQL Type due to error. ${msg}")
      }
  }

  case class Product(fields: mutable.LinkedHashMap[String, Quat]) extends Probity {

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

    def leastUpperTypeProduct2(other: Quat.Product): Option[Quat.Product] = {
      val newFields =
        fields.zipWith(other.fields) {
          case (key, thisQuat, Some(otherQuat)) => (key, thisQuat.leastUpperType2(otherQuat))
        }.collect {
          case (key, Some(value)) => (key, value)
        }
      Some(Quat.Product(newFields))
    }

    def leastUpperTypeProduct(other: Quat.Product): Option[Quat.Product] = {
      // TODO Quat this is a N^2 field check. Explor changing product fields into ListMap and make this more efficient

      val newFields = collection.mutable.ArrayBuffer[(String, Quat)]()
      for ((otherField, otherValue) <- other.fields) {
        val foundField =
          this.fields
            .find(tup => tup._1 == otherField)
            .flatMap {
              case (thisField, thisValue) =>
                thisValue.leastUpperType(otherValue).map(v => (thisField, v))
            }

        // Early exit if any of the fields are None for performance reasons
        if (foundField.isEmpty)
          return None

        newFields += ((foundField.head))
      }
      Some(Quat.Product(newFields))
    }

    override def withRenames(renames: List[(String, String)]) =
      Product.WithRenames(fields, renames)

    /**
     * Rename the properties and reset renames to empty.
     * An interesting idea would be not to clear
     * the renames at the end but rather keep them around until the SqlQuery phase wherein we could potentially
     * create SQL aliases not based on the renamed properties by based on the original property name.
     */
    override def applyRenames: Quat.Product = {
      val newFields = fields.map {
        case (f, q) =>
          // Rename properties of this quat and rename it's children recursively
          val newKey = renames.find(_._1 == f).map(_._2).getOrElse(f)
          val newValue = q.applyRenames
          (newKey, newValue)
      }
      Product(newFields)
    }

    /** Rename a property of a Quat.Tuple or Quat.CaseClass. Optionally can specify a new Quat to change the property to. */
    def stashRename(property: String, newProperty: String, newQuat: Option[Quat] = None): Quat.Probity = {
      // sanity check does the property exist in the first place
      this.lookup(property) match {
        case e: Quat.Error => e
        case _ => {
          (this, property, newProperty) match {
            case (cc: Quat.Product, property, newProperty) =>
              cc.fields.get(property) match {
                case Some(_) =>
                  // Note. Technically this is an N^2 behavior and may not be suitable for mass renaming of large records (e.g. hundreds of fields).
                  val newFields =
                    cc.fields.map {
                      case (field, quat) => if (field == property) (field, newQuat.getOrElse(quat)) else (field, quat)
                    }
                  Quat.Product.WithRenames(newFields, cc.renames :+ ((property, newProperty)))

                case None =>
                  cc
              }
            case _ => Quat.Error(s"Invalid state reached for ${this.shortString}, ${property}, ${newProperty}")
          }
        }
      }
    }

    def stashRename(path: List[String], newEndProperty: String): Quat.Probity = {
      path match {
        case Nil => // do nothing
          this

        case head :: Nil =>
          this.stashRename(head, newEndProperty)

        case head :: tail =>
          val child = this.lookup(head)
          val newChild: Either[Quat.Error, Probity] =
            child match {
              case childProduct: Quat.Product =>
                Right(childProduct.stashRename(tail, newEndProperty))
              case e: Quat.Error =>
                Left(e)
              case other =>
                Left(Quat.Error(s"Cannot continue to lookup property '${tail.head}' from ${other.shortString} in ${this.shortString}"))
            }

          newChild match {
            case Right(value) => this.stashRename(head, head, Some(value))
            case Left(error)  => error
          }
      }
    }
  }
  def LeafProduct(list: String*) = Quat.Product(list.map(e => (e, Quat.Value)))
  def LeafTuple(numElems: Int) = Quat.Tuple((1 to numElems).map(_ => Quat.Value))

  object Product {
    def empty = new Quat.Product(mutable.LinkedHashMap[String, Quat]())
    def apply(fields: (String, Quat)*): Quat.Product = apply(fields.iterator)
    def apply(fields: Iterable[(String, Quat)]): Quat.Product = new Quat.Product(fields.iterator)
    def apply(fields: Iterator[(String, Quat)]): Quat.Product = new Quat.Product(fields)
    def unapply(p: Quat.Product): Some[(List[(String, Quat)])] = Some(p.fields.toList)

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
        case _   => Quat.Error(s"Renames ${renames} cannot be applied to a value SQL-level type")
      }
  }
  case class Error(msg: String, immediateThrow: Boolean = true) extends Probity with Quat {
    if (immediateThrow)
      throw new QuatException(msg)
    override def withRenames(renames: List[(String, String)]): Quat = this
  }
}
