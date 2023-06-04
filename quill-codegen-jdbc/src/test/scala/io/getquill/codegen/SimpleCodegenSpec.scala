package io.getquill.codegen.codegen

import io.getquill.codegen.util.StringUtil._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

class SimpleCodegenSpec extends AnyFreeSpec with Matchers {

  case class FieldData(fieldName: String, dataType: String, columnName: String)
  case class FieldDataGroup(data: FieldData*) {
    def ccList = data.map(fd => (fd.fieldName, fd.dataType))

    def querySchemaList = data.map(fd => (fd.fieldName, fd.columnName))
  }

  def fdg(data: FieldData*) = FieldDataGroup(data: _*)

  def fdgConv(data: (String, String)*)(converter: String => String) =
    FieldDataGroup(data.map { case (field, dataType) =>
      FieldData(field, dataType, converter(field))
    }: _*)

  case class QuerySchema(defName: String, tableName: String, fields: Seq[(String, String)])

  def assertStandardObject(objectCode: String, objectName: String, ccName: String, querySchemas: Seq[QuerySchema]) = {
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox()
    val ob = tb.parse(objectCode)
    ob match {
      case q"$mods object $otname extends { ..$earlydefns } with ..$parents { $self => ..$body }" => {
        assert(otname.toString == objectName, s"Object name $otname incorrect")

        if (body.length != querySchemas.length)
          fail(
            s"Different number of params (${querySchemas.length} vs ${body.length}) in object $otname then expected. " +
              s"Expected results are $body vs ${querySchemas.map(_.defName)}"
          )

        querySchemas.zip(body).foreach {
          case (schema, methodTree) => {
            methodTree match {
              case q"$mods def $tname(...$params): $tpt = $expr" => {
                assert(tname.toString.unquote == schema.defName, s"Def method ${tname} should be ${schema.defName}")
                assert(params.length == 0, s"Def method ${tname} should not have any params for $tname")

                val quotedExpr = expr match {
                  case q"quote { $qs_args }" => {
                    qs_args
                  }
                  case _ => fail(s"Quoted block expected inside of ${expr}")
                }

                quotedExpr match {
                  case q"querySchema[..$qs_tpts](...$qs_args)" => {
                    val locName = s"schema $tname of object $otname"
                    assert(
                      qs_tpts.length == 1,
                      s"Number of parameters must be 1 for all query schemas but in $locName it was ${qs_tpts.length}"
                    )
                    assert(
                      qs_tpts.toList(0).toString() == ccName,
                      s"Incorrect template ${qs_tpts.toList(0)} in ${locName}. It should be ${ccName}"
                    )
                    assert(
                      qs_args.length == 1,
                      s"Query Schema list at ${locName} should only have one set of parameters but doesn't."
                    )
                    val args: Seq[_] = qs_args.toList(0).toList

                    assert(
                      args.length == schema.fields.length + 1,
                      s"Incorrect number of parameters (${schema.fields.length + 1} vs ${qs_args.length}) in query schema application in ${locName}."
                        + s"Should be ${schema.fields} but was $qs_args"
                    )

                    val err =
                      s"Cannot match expression head querySchema expression ${args.toList.head} to $locName in schema def ${schema.defName}"
                    args.toList.head match {
                      case q"$value" => assert(value.toString.unquote == schema.tableName, err)
                    }

                    schema.fields
                      .zip(args.toList.tail)
                      .foreach { case ((field, tableColumn), schemaFieldTree) =>
                        schemaFieldTree match {
                          case q"(..$params) => $expr" => {
                            val err =
                              s"Cannot match expression ${expr} to $field/$tableColumn in $locName in schema def ${schema.defName}"
                            expr match {
                              case q"$inputArg.$mapRhs -> $mapLhs" => {
                                assert(mapRhs.toString == field, err)
                                assert(mapLhs.toString.unquote == tableColumn, err)
                              }
                              case _ => fail(err)
                            }
                          }
                          case _ =>
                            fail(s"Invalid datatype in schema definition ${locName} in schema ${schema.defName}")
                        }
                      }
                  }
                }
              }
              case _ => fail(s"Pattern match for schema member ${schema.defName} failed in $otname")
            }
          }
        }
      }
      case _ => fail(s"Object code does not match objectCode pattern in  ${objectCode}")
    }
  }

  def assertCaseClass(generatedCode: String, className: String, fields: Seq[(String, String)]) = {
    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox()
    val cc = tb.parse(generatedCode)
    cc match {
      case q"case class $tpname(...$params) extends { ..$earlydefns } with ..$parents" => {
        tpname.toString() should equal(className)
        val constructorList = params
        if (constructorList.length != 1) fail(s"Class $tpname has more then one constructor list")
        val paramList: Seq[_] = constructorList.toList(0).toList
        if (paramList.length != fields.length)
          fail(s"List of fields did not match list of case class params in ${tpname}")

        fields.zip(paramList).foreach { case ((fieldName, fieldType), fieldTree) =>
          fieldTree match {
            case q"$mods val $name: $tpe = $rhs" => {
              assert(name.toString == fieldName, s"Field ${name.toString} does not match ${fieldName} in $tpname")
              assert(
                tpe.toString == fieldType,
                s"Field Type ${tpe.toString} does not match ${fieldType} in field: $fieldName in $tpname"
              )
            }
            case _ => fail(s"Parsed Tree $fieldTree did not match Field/Type ($fieldName, $fieldType)")
          }
        }
      }
      case _ => fail(s"Case class does not pattern match: ${generatedCode}")
    }
  }
}
