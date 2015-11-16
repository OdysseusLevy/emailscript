package org.emailscript.helpers

import org.xbill.DNS.{Lookup, Type}

/**
 * Hide from the rest of the system how we are doing dns queries.
 * This let's us change the implementation if we want
 */
class DnsHelper {

  private def toType(dnsType: String) = {

    dnsType.toLowerCase match {
      case "txt" => Type.TXT
      case "mx" => Type.MX
      case "a" => Type.A
      case _ => Type.ANY
    }
  }

  def getDnsRecords(domain: String, dnsType: String): Array[String] = {

    val records = new Lookup(domain, toType(dnsType)).run()

    if (records == null)
      Array()
    else
      records.map{_.rdataToString()}
  }
}
