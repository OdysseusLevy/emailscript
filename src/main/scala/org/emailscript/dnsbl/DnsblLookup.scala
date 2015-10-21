package org.emailscript.dnsbl

import org.emailscript.helpers.LoggerFactory
import org.xbill.DNS._

import scala.collection.mutable.Map

object DnsblResult {
  val empty = DnsblResult("None", "None", "None", "None")
  override def toString = "none"
}

case class DnsblResult(name: String, lookupName: String, result: String, description: String)

abstract class DnsblLookup {

  val logger = LoggerFactory.getLogger(getClass)
  val cache: Map[String, DnsblResult] = Map()

  def getLookupName(host: String): String
  def getDescriptionForResult(result: String): String

  private def doDnsLookup(lookupName: String, host: String): DnsblResult = {
    try {
      val lookup: Lookup = new Lookup(lookupName)
      lookup.setSearchPath(null.asInstanceOf[Array[String]]) // Don'keepAlive want extra lookups if we get a NXDOMAIN result

      val records: Array[Record] = lookup.run

      if (records == null || records.size == 0 )
        DnsblResult.empty
      else {
        val record: ARecord = records(0).asInstanceOf[ARecord]
        val result = record.rdataToString
        DnsblResult(host, lookupName, result, getDescriptionForResult(result))
      }
    }
    catch {
      case e: Throwable => {
        logger.warn(s"problem with looking up host: $host error: ${e.getMessage}")
        DnsblResult.empty
      }
    }
  }

  def checkDNBSL(host: String): DnsblResult = {

    val lookupName = getLookupName(host)

    val cachedResult = cache.get(lookupName)
    if (cachedResult.isDefined) {
      return cachedResult.get
    }

    val result = doDnsLookup(lookupName, host)
    cache.put(lookupName,result)

    result
  }
}

class MultipleDnsblLookup(dnsbls: Iterable[DnsblLookup]) extends DnsblLookup {

  override val logger = LoggerFactory.getLogger(getClass)
  override def checkDNBSL(host: String): DnsblResult = {

    dnsbls.foreach{ dnsbl =>
      val result = dnsbl.checkDNBSL(host)
      if (result != DnsblResult.empty)
        return result
    }

    DnsblResult.empty
  }

  override def getDescriptionForResult(result: String): String = "Undefined" // never called
  override def getLookupName(host: String): String = "Undefined" // never called
}
