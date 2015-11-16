package org.emailscript.helpers

trait Importer {
  def doImport(): AnyRef
}

trait Exporter {
  def doExport(): AnyRef
}

object Serializer{
  def convertForExport(data: AnyRef): AnyRef = {
    data match {
      case e: Exporter => e.doExport()
      case _ => data
    }
  }

  def convertForImport(data: AnyRef): AnyRef = {
    data match {
      case i: Importer => i.doImport()
      case _ => data
    }
  }
}
