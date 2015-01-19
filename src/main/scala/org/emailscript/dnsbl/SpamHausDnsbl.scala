package org.emailscript.dnsbl

object SpamHausDnsbl {
  val SpamHausHost = "dbl.spamhaus.org"

  def apply() = new SpamHausDnsbl
}

class SpamHausDnsbl extends DnsblLookup{

  override def getDescriptionForResult(result: String): String = {
    result match {
      case "127.0.1.1" => "Spam domain (SpamHaus)"
      case "127.0.1.2" => "spammed redirector domain (SpamHaus)"
      case _ => "Unrecognized result"
    }
  }

    override def getLookupName(host: String): String = {
    host + "." + SpamHausDnsbl.SpamHausHost
  }

}
