package io.getquill.util

object Show {
  trait Show[T] {
    def show(v: T): String
  }

  object Show {
    def apply[T](f: T => String) = new Show[T] {
      def show(v: T) = f(v)
    }
  }

  implicit class Shower[T](v: T)(implicit shower: Show[T]) {
    def show = shower.show(v)
  }

  implicit def listShow[T](implicit shower: Show[T]) = Show[List[T]] {
    case list if !list.exists(_.toString.contains("JoinSource(")) =>
	    list.map(_.show).mkString(", ")
    case joinParts =>
    	val xs = joinParts.map(_.show)
    	//xs foreach println
    	val parts = 
    		if(xs.size > 1) {
    			// head in format: table x1
    			// tail in format: JOIN table t ON t.id = a.id
    			val tail = xs.tail. // strip leading table(s)
	    			flatMap(_.split(joinTypes).toList).
						map(_.trim)
					tail foreach println
						
	    		xs.head :: 
	    		xs.tail. // strip leading table(s)
	    			flatMap(_.split(joinTypes).toList.tail).
						map(_.trim)
	    	} else xs
    	parts.mkString(" ")
  }
  private val joinTypes = "INNER|LEFT|RIGHT|FULL"
}
