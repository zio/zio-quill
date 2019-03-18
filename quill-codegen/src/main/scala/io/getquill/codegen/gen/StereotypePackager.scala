package io.getquill.codegen.gen

import io.getquill.codegen.model._

/**
 * Packages stereotyped tables into emitters.
 */
class StereotypePackager[Emitter, TableMeta, ColumnMeta] {

  def packageIntoEmitters(
    emitterMaker:      (EmitterSettings[TableMeta, ColumnMeta]) => Emitter,
    packagingStrategy: PackagingStrategy,
    tables:            Seq[TableStereotype[TableMeta, ColumnMeta]]
  ) =
    {
      import io.getquill.codegen.util.MapExtensions._
      import packagingStrategy._
      implicit class OptionSeqExtentions[T](optionalSeq: Option[Seq[T]]) {
        def toSeq = optionalSeq match {
          case Some(seq) => seq
          case None      => Seq[T]()
        }
      }

      // Generate (Packaging, [TableA, TableB, TableC]) objects for case classes
      val caseClassDefs = groupByNamespace(tables, packageNamingStrategyForCaseClasses)
      // Generate (Packaging, [TableA, TableB, TableC]) objects for query schemas
      val querySchemaDefs = groupByNamespace(tables, packageNamingStrategyForQuerySchemas)
      // Merge the two
      val mergedDefinitions =
        caseClassDefs.zipOnKeys(querySchemaDefs)
          .map({ case (wrap, (caseClassOpt, querySchemaOpt)) => (wrap, (caseClassOpt.toSeq, querySchemaOpt.toSeq)) })
          .map({
            case (wrap, (caseClassSeq, querySchemaSeq)) =>
              makeCodegenForNamespace(
                emitterMaker,
                packageGroupingStrategy, wrap, caseClassSeq, querySchemaSeq
              )
          })

      mergedDefinitions.toSeq.flatMap(s => s)
    }

  protected def makeCodegenForNamespace[H >: Emitter](
    generatorMaker:    (EmitterSettings[TableMeta, ColumnMeta]) => H,
    groupingStrategy:  PackageGroupingStrategy,
    wrapping:          CodeWrapper,
    caseClassTables:   Seq[TableStereotype[TableMeta, ColumnMeta]],
    querySchemaTables: Seq[TableStereotype[TableMeta, ColumnMeta]]
  ): Seq[H] = {

    groupingStrategy match {
      // if we are not supposed to group, create a unique codegen for every
      // [TableA, TableB, TableC] => { case class TableA(...); object TableA {querySchemaA, querySchemaB, etc...} } { case class TableB... }
      case DoNotGroup => {
        (caseClassTables ++ querySchemaTables)
          .groupBy(tc => tc)
          .map(_._1)
          .map(tc => generatorMaker(EmitterSettings(Seq(tc), Seq(tc), wrapping)))
          .toSeq
      }
      case GroupByPackage =>
        Seq(generatorMaker(EmitterSettings(caseClassTables, querySchemaTables, wrapping)))
    }
  }

  def groupByNamespace(
    tables:                Seq[TableStereotype[TableMeta, ColumnMeta]],
    packageNamingStrategy: PackageNamingStrategy
  ): Map[CodeWrapper, Seq[TableStereotype[TableMeta, ColumnMeta]]] = {
    val mapped = tables.map(tbl => (packageNamingStrategy.apply(tbl), tbl))
    mapped
      .groupBy({ case (pack, _) => pack })
      .map({ case (pack, seq) => (pack, seq.map(_._2)) })
  }

}
