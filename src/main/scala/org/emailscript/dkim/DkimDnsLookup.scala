package org.emailscript.dkim

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}
import java.util.Base64

import org.emailscript.helpers.{DnsHelper, LoggerFactory}

object DkimDnsLookup {
  val logger = LoggerFactory.getLogger(getClass)

  val DKIM1 = "DKIM1"
  val RSA = "rsa"

  def removeWhiteSpace(text: String) = text.replaceAll("""[  \t\n\r"]""", "")

  // Remove beginning and end quotes, if any
  // Technically, should not be legal, but it seems common in DNS records
  def deQuote(text: String): String = {

    val start = if (text.startsWith("\"")) 1 else 0
    val end = if (text.endsWith("\"")) text.length - 1 else text.length

    text.substring(start, end)
  }
}

class DkimDnsLookup(helper: DnsHelper) {
  import DkimDnsLookup._

  // Find (and create) the first valid key we find
  def getPublicKey(dnsHost: String): Option[PublicKey] = {
     helper.getDnsRecords(dnsHost, "TXT").flatMap{createKey}.headOption
  }


  private def createKey(record: String): Option[PublicKey] = {

    try {
      logger.debug(s"dns record: $record")
      val fields = DkimSignature.mapFields(deQuote(record))
      validate(fields).flatMap(generatePublicKey)
    } catch {
      case error: Throwable =>
        logger.debug(s"invalid dns entry: error: ${error.getMessage} $record")
        None
    }
  }

  private def validate(map: Map[String, String]): Option[String] = {
    if (map.getOrElse("v", DKIM1) != DKIM1)
      None
    else map.get("p")
  }

  private def generatePublicKey(encodedPublicKey: String): Option[PublicKey] = {

      logger.debug(s"encoded key: $encodedPublicKey")

      val keyText = removeWhiteSpace(encodedPublicKey)
      val decodedKey = Base64.getDecoder().decode(keyText)
      val keyFactory = KeyFactory.getInstance("RSA")
      Some(keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey)))
  }

}