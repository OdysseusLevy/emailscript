package org.emailscript.dkim

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}
import java.util.Base64

import org.emailscript.helpers.{DnsHelper, LoggerFactory}

import scala.collection.concurrent.RDCSS_Descriptor

object DkimDnsLookup {
  val logger = LoggerFactory.getLogger(getClass)

  val DKIM1 = "DKIM1"
  val RSA = "rsa"

  def removeWhiteSpace(text: String) = text.replaceAll("""[  \t\n\r"]""", "")

}

class DkimDnsLookup(helper: DnsHelper) {
  import DkimDnsLookup._

  // Find (and create) the first valid key we find
  def getPublicKey(dnsHost: String): PublicKey = {
    val records = helper.getDnsRecords(dnsHost, "TXT")
    if (records.length == 0)
      throw new Exception(s"No TXT records found in DNS entry for : $dnsHost")

    val maps = records.map{record: String => DkimSignature.mapFields(removeWhiteSpace(record))}

    val mapOption = maps.find(isValid(_))

    if (mapOption.isEmpty){
      val recordText = records.mkString(",")
      throw new Exception(s"No valid TXT record found for $dnsHost, records: $recordText")
    }

    val fieldMap = mapOption.get
    generatePublicKey(fieldMap.get("p").get)
  }


  private def isValid(map: Map[String, String]): Boolean = {
    if (map.getOrElse("v", DKIM1) != DKIM1)
      false
    else if (!map.contains("p"))
      false
    else
      true
  }

  private def generatePublicKey(encodedPublicKey: String): PublicKey = {

      logger.debug(s"encoded key: $encodedPublicKey")

      val decodedKey = Base64.getDecoder().decode(encodedPublicKey)
      val keyFactory = KeyFactory.getInstance("RSA")
      keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey))
  }

}