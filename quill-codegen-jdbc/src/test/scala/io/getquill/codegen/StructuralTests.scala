package io.getquill.codegen

import io.getquill.codegen.codegen.SimpleCodegenSpec
import io.getquill.codegen.util.StringUtil._
import com.typesafe.scalalogging.Logger
import io.getquill.codegen.model.{CustomNames, SnakeCaseCustomTable, SnakeCaseNames}
import io.getquill.codegen.util.SchemaConfig._

class StructuralTests extends SimpleCodegenSpec with WithStandardCodegen {

  def LOG                               = Logger(getClass)
  override def defaultNamespace: String = "schema"

  "simple end to end tests" - {

    "snake case naming strategy" - {
      val personData =
        fdgConv("id" -> "Int", "firstName" -> "Option[String]", "lastName" -> "Option[String]", "age" -> "Int")(
          _.lowerCamelToSnake.toUpperCase
        )
      val addressData = fdgConv("personFk" -> "Int", "street" -> "Option[String]", "zip" -> "Option[Int]")(
        _.lowerCamelToSnake.toUpperCase
      )

      object SnakeCaseNamesWithSchema extends SnakeCaseNames {
        override def generateQuerySchemas: Boolean = true
      }

      "single table" in {
        val gens = standardCodegen(
          `schema_snakecase`,
          _.table.tableName.toLowerCase == "person",
          SnakeCaseNamesWithSchema
        ).makeGenerators.toList

        LOG.info(gens(0).tableSchemasCode)

        assertCaseClass(gens(0).caseClassesCode, "Person", personData.ccList)
        assertStandardObject(
          gens(0).tableSchemasCode,
          "Person",
          "Person",
          Seq(QuerySchema("person", "PUBLIC.PERSON", List()))
        )
      }

      "multi table" in {
        val gens = standardCodegen(
          `schema_snakecase`,
          entityNamingStrategy = SnakeCaseNamesWithSchema
        ).makeGenerators.toList.sortBy(_.caseClassesCode)

        assertCaseClass(gens(0).caseClassesCode, "Address", addressData.ccList)
        assertStandardObject(
          gens(0).tableSchemasCode,
          "Address",
          "Address",
          Seq(QuerySchema("address", "PUBLIC.ADDRESS", List()))
        )

        assertCaseClass(gens(1).caseClassesCode, "Person", personData.ccList)
        assertStandardObject(
          gens(1).tableSchemasCode,
          "Person",
          "Person",
          Seq(QuerySchema("person", "PUBLIC.PERSON", List()))
        )
      }
    }

    "custom naming strateogy" - {

      val personData =
        fdgConv("id" -> "Int", "firstname" -> "Option[String]", "lastname" -> "Option[String]", "age" -> "Int")(
          _.toUpperCase
        )
      val addressData =
        fdgConv("personfk" -> "Int", "street" -> "Option[String]", "zip" -> "Option[Int]")(_.toUpperCase)

      "single table" in {

        val gens = standardCodegen(
          `schema_simple`,
          _.table.tableName.toLowerCase == "person",
          CustomNames(c => c.columnName.toLowerCase, s => s.tableName.toLowerCase)
        ).makeGenerators.toList

        assertCaseClass(gens(0).caseClassesCode, "person", personData.ccList)
        assertStandardObject(
          gens(0).tableSchemasCode,
          "person",
          "person",
          Seq(QuerySchema("person", "PUBLIC.PERSON", personData.querySchemaList))
        )
      }

      "multi table" in {

        val gens = standardCodegen(
          `schema_simple`,
          entityNamingStrategy = CustomNames(c => c.columnName.toLowerCase, s => s.tableName.toLowerCase)
        ).makeGenerators.toList.sortBy(_.caseClassesCode)

        assertCaseClass(gens(0).caseClassesCode, "address", addressData.ccList)
        assertStandardObject(
          gens(0).tableSchemasCode,
          "address",
          "address",
          Seq(QuerySchema("address", "PUBLIC.ADDRESS", addressData.querySchemaList))
        )

        assertCaseClass(gens(1).caseClassesCode, "person", personData.ccList)
        assertStandardObject(
          gens(1).tableSchemasCode,
          "person",
          "person",
          Seq(QuerySchema("person", "PUBLIC.PERSON", personData.querySchemaList))
        )
      }
    }

  }

  "collision end to end tests" - {

    "custom naming" - {
      val personData =
        fdgConv("id" -> "Int", "firstname" -> "Option[String]", "lastname" -> "Option[String]", "age" -> "Int")(
          _.toUpperCase
        )
      val addressData =
        fdgConv("personfk" -> "Int", "street" -> "Option[String]", "zip" -> "Option[Int]")(_.toUpperCase)

      "prefix collision" in {
        val gens = standardCodegen(
          `schema_twotable`,
          entityNamingStrategy = CustomNames(
            c => c.columnName.toLowerCase,
            s => {
              s.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", "")
            }
          )
        ).makeGenerators.toList.sortBy(_.caseClassesCode)

        assertCaseClass(gens(0).caseClassesCode, "address", addressData.ccList)
        assertStandardObject(
          gens(0).tableSchemasCode,
          "address",
          "address",
          Seq(QuerySchema("address", "PUBLIC.ADDRESS", addressData.querySchemaList))
        )

        assertCaseClass(gens(1).caseClassesCode, "person", personData.ccList)
        assertStandardObject(
          gens(1).tableSchemasCode,
          "person",
          "person",
          Seq(
            QuerySchema("alphaPerson", "PUBLIC.ALPHA_PERSON", personData.querySchemaList),
            QuerySchema("bravoPerson", "PUBLIC.BRAVO_PERSON", personData.querySchemaList)
          )
        )
      }
    }

    "with snake schema" - {

      "prefix collision - different columns without datatype percolation" - {

        val personData =
          fdgConv("id" -> "Int", "firstName" -> "Option[String]", "lastName" -> "Option[String]", "age" -> "Int")(
            _.lowerCamelToSnake.toUpperCase
          )
        val addressData = fdgConv("personFk" -> "Int", "street" -> "Option[String]", "zip" -> "Option[Int]")(
          _.lowerCamelToSnake.toUpperCase
        )

        "prefix test with snake case" in {
          val gens = standardCodegen(
            `schema_snakecase_twotable`,
            entityNamingStrategy = SnakeCaseCustomTable(_.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", ""))
          ).makeGenerators.toList.sortBy(_.caseClassesCode)

          gens.foreach(gen => LOG.info(gen.code))

          assertCaseClass(gens(0).caseClassesCode, "address", addressData.ccList)
          assertStandardObject(
            gens(0).tableSchemasCode,
            "address",
            "address",
            Seq(QuerySchema("address", "PUBLIC.ADDRESS", List()))
          )

          assertCaseClass(gens(1).caseClassesCode, "person", personData.ccList)
          assertStandardObject(
            gens(1).tableSchemasCode,
            "person",
            "person",
            Seq(
              QuerySchema("alphaPerson", "PUBLIC.ALPHA_PERSON", List()),
              QuerySchema("bravoPerson", "PUBLIC.BRAVO_PERSON", List())
            )
          )
        }

        "prefix collision - different columns with datatype percolation" in {
          val gens = standardCodegen(
            `schema_snakecase_twotable_differentcolumns`,
            entityNamingStrategy = SnakeCaseCustomTable(_.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", ""))
          ).makeGenerators.toList.sortBy(_.caseClassesCode)

          gens.foreach(gen => LOG.info(gen.code))

          assertCaseClass(gens(0).caseClassesCode, "address", addressData.ccList)
          assertStandardObject(
            gens(0).tableSchemasCode,
            "address",
            "address",
            Seq(QuerySchema("address", "PUBLIC.ADDRESS", List()))
          )

          assertCaseClass(gens(1).caseClassesCode, "person", personData.ccList)
          assertStandardObject(
            gens(1).tableSchemasCode,
            "person",
            "person",
            Seq(
              QuerySchema("alphaPerson", "PUBLIC.ALPHA_PERSON", List()),
              QuerySchema("bravoPerson", "PUBLIC.BRAVO_PERSON", List())
            )
          )
        }
      }

      "prefix collision - different columns with datatype percolation" - {

        val personData = fdgConv(
          "id"          -> "Int",
          "firstName"   -> "Option[String]",
          "lastName"    -> "Option[String]",
          "age"         -> "Int",
          "numTrinkets" -> "Option[Long]",
          "trinketType" -> "String"
        )(_.lowerCamelToSnake.toUpperCase)

        val addressData =
          fdgConv("personFk" -> "Int", "street" -> "Option[String]", "zip" -> "Int")(_.lowerCamelToSnake.toUpperCase)

        "prefix test with snake case - with different columns - and different types" in {
          val gens = standardCodegen(
            `schema_snakecase_twotable_differentcolumns_differenttypes`,
            entityNamingStrategy =
              SnakeCaseCustomTable(_.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", "").capitalize)
          ).makeGenerators.toList.sortBy(_.caseClassesCode)

          gens.foreach(gen => LOG.info(gen.code))

          assertCaseClass(gens(0).caseClassesCode, "Address", addressData.ccList)
          assertStandardObject(
            gens(0).tableSchemasCode,
            "Address",
            "Address",
            Seq(QuerySchema("address", "PUBLIC.ADDRESS", List()))
          )

          assertCaseClass(gens(1).caseClassesCode, "Person", personData.ccList)
          assertStandardObject(
            gens(1).tableSchemasCode,
            "Person",
            "Person",
            Seq(
              QuerySchema("alphaPerson", "PUBLIC.ALPHA_PERSON", List()),
              QuerySchema("bravoPerson", "PUBLIC.BRAVO_PERSON", List())
            )
          )
        }
      }

      "namespace collision - different columns with datatype percolation" - {

        val personData = fdgConv(
          "id"          -> "Int",
          "firstName"   -> "Option[String]",
          "lastName"    -> "Option[String]",
          "age"         -> "Int",
          "numTrinkets" -> "Option[Long]",
          "trinketType" -> "String"
        )(_.lowerCamelToSnake.toUpperCase)

        val addressData =
          fdgConv("personFk" -> "Int", "street" -> "Option[String]", "zip" -> "Int")(_.lowerCamelToSnake.toUpperCase)

        "prefix test with snake case - with different columns - and different types" in {
          val gens = standardCodegen(
            `schema_snakecase_twoschema_differentcolumns_differenttypes`,
            entityNamingStrategy =
              SnakeCaseCustomTable(_.tableName.toLowerCase.replaceFirst("(alpha_)|(bravo_)", "").capitalize),
            entityNamespacer =
              _.tableSchema.map(_.toLowerCase.replaceAll("(alpha)|(bravo)", "public")).getOrElse(this.defaultNamespace),
            entityMemberNamer = ts => s"${ts.tableSchema.get}_${ts.tableName}".toLowerCase.snakeToLowerCamel
          ).makeGenerators.toList.sortBy(_.caseClassesCode)

          gens.foreach(gen => LOG.info(gen.code))

          assertCaseClass(gens(0).caseClassesCode, "Address", addressData.ccList)
          assertStandardObject(
            gens(0).tableSchemasCode,
            "Address",
            "Address",
            Seq(QuerySchema("publicAddress", "PUBLIC.ADDRESS", List()))
          )

          assertCaseClass(gens(1).caseClassesCode, "Person", personData.ccList)
          assertStandardObject(
            gens(1).tableSchemasCode,
            "Person",
            "Person",
            Seq(
              QuerySchema("bravoPerson", "BRAVO.PERSON", List()),
              QuerySchema("alphaPerson", "ALPHA.PERSON", List())
            )
          )
        }
      }
    }
  }
}
