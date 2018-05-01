package io.getquill.context.spark.norm

import io.getquill.ast.{ Infix, ScalarValueLift }

class InfixExtractor(i: Infix, id: String) {
  val universe: scala.reflect.runtime.universe.type = scala.reflect.runtime.universe
  import universe._

  import scala.util.{ Failure => MFailure, Success => MSuccess, Try => MTry }

  protected def firstConstructorParamList(tpe: Type): Option[List[String]] = {
    val paramLists = tpe.decls.collect {
      case m: MethodSymbol if m.isConstructor => m.paramLists.map(_.map(_.name))
    }
    for {
      paramList <- paramLists.toList.headOption
      firstParamList <- paramList.headOption
    } yield (firstParamList.map(_.toString))
  }

  protected def firstTypeArgInMethodSymbol(sym: Symbol): Option[Type] =
    (for {
      method <- MTry(sym.asInstanceOf[MethodSymbol])
      tpe <- MTry(method.returnType.asInstanceOf[TypeRef])
      firstArg <- MTry(tpe.typeArgs(0))
    } yield (firstArg)) match {
      case MSuccess(value) => Some(value)
      case MFailure(_)     => None
    }

  protected def extractLiftValue(value: Any) = value match {
    case q"$v.value" => Some(v)
    case _           => None
  }

  protected def isDataset(tpe: Type) =
    tpe.typeSymbol.fullName == weakTypeOf[org.apache.spark.sql.Dataset[_]].typeSymbol.fullName

  protected def extractDatasetParameter(tpe: Type) =
    if (isDataset(tpe)) Some(tpe.typeArgs(0)) else None

  protected def findTypeArgsFromDataset =
    for {
      scalarValue <- i.params.headOption.collect({ case ScalarValueLift(_, value, _) => value })
      liftValue <- extractLiftValue(scalarValue)
      firstTypeArg <- firstTypeArgInMethodSymbol(liftValue.symbol)
      datasetClass <- extractDatasetParameter(firstTypeArg)
      typeArgs <- firstConstructorParamList(datasetClass)
    } yield (typeArgs)

  def extract: SearchState = {
    // Last resort, catch any possible exception that could have happened from the retrieval
    val outputState =
      MTry(findTypeArgsFromDataset) match {
        case MSuccess(value) => value.map(propertyList => FoundEntityIds(id, propertyList))
        case MFailure(_)     => None
      }

    outputState match {
      case Some(value) => value
      case None        => NotFound
    }
  }
}

object InfixExtractor {
  def apply(i: Infix, id: String) = new InfixExtractor(i, id).extract
}