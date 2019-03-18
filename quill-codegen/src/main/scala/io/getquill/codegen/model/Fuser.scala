package io.getquill.codegen.model

import io.getquill.codegen.util.MapExtensions._
import io.getquill.codegen.dag.CatalogBasedAncestry
import io.getquill.codegen.dag.dag.ClassAncestry
import io.getquill.codegen.model.Stereotyper.Fuser

import scala.collection.immutable.ListMap

trait FuserBase[TableMeta, ColumnMeta] extends Fuser[TableMeta, ColumnMeta] {

  val ancestry: ClassAncestry

  protected def unifyColumns(a: ColumnFusion[ColumnMeta], b: ColumnFusion[ColumnMeta]) = {
    val commonAncestor = ancestry(a.dataType, b.dataType)
    ColumnFusion(
      a.name,
      commonAncestor,
      a.nullable || b.nullable,
      a.meta ++ b.meta
    )
  }

  protected def fuseColumns(a: Seq[ColumnFusion[ColumnMeta]], b: Seq[ColumnFusion[ColumnMeta]]): Seq[ColumnFusion[ColumnMeta]] = {
    // join the two sets of columns by name, take only the ones with columns in common
    // and then unify the columns.
    val aOrdered = new ListMap() ++ a.map(c => (c.name, c))
    val bOrdered = new ListMap() ++ b.map(c => (c.name, c))

    aOrdered.zipOnKeysOrdered(bOrdered)
      .map({ case (_, column) => column })
      .collect {
        case (Some(a), Some(b)) => unifyColumns(a, b)
      }.toSeq
  }

  /**
   * Take all the variations of a table, find the columns that they have in common and fuse
   * all the columns . Return the list of fused columns.
   */
  protected def fuseTableVariations(variations: Seq[TableStereotype[TableMeta, ColumnMeta]]): Seq[ColumnFusion[ColumnMeta]] = {
    variations
      .map(_.columns)
      .reduce((a, b) => fuseColumns(a, b))
  }
}

class DefaultFuser[TableMeta, ColumnMeta](val ancestry: ClassAncestry = new CatalogBasedAncestry()) extends FuserBase[TableMeta, ColumnMeta] {
  override def apply(collidingTables: Seq[TableStereotype[TableMeta, ColumnMeta]]): TableStereotype[TableMeta, ColumnMeta] = {
    TableStereotype(
      // Grab all the table schemas from the tables merged since we might want to keep track of them later
      collidingTables.head.table.copy(
        meta = collidingTables.flatMap(_.table.meta)
      ),
      fuseTableVariations(collidingTables)
    )
  }
}
