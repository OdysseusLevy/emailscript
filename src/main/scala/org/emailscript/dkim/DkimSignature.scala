package org.emailscript.dkim

import org.emailscript.helpers.LoggerFactory

object DkimSignature {

  val empty = apply()

  def apply(rawSignature: String = "",
             hashAlgorithm: String = "",
             signature: String = "",
             bodyHash: String = "",
             domain: String = "",
             headers: String = "",
             dnsSelector: String = "",
             time: Long = 0,
             expiration: Long = 0,
             headerCanonicalization: String = "",
             bodyCanonicalization: String = "",
             identity: String = "",
             copiedHeaders: String = "",
             bodyLength: Int = 0) = {

    new DkimSignature(
      rawSignature = rawSignature, hashAlgorithm = hashAlgorithm, signature = signature, bodyHash = bodyHash,
      domain = domain, headers = headers,
      dnsSelector = dnsSelector, time = time, expiration = expiration,
      headerCanonicalization = headerCanonicalization, bodyCanonicalization = bodyCanonicalization,
      identity = identity, copiedHeaders = copiedHeaders, bodyLength = bodyLength)
  }
  val DkimHeader = "Dkim-Signature"

  val logger = LoggerFactory.getLogger(getClass)
  def Simple = "simple"
  def Relaxed = "relaxed"

  def SHA1 = "rsa-sha1"
  def SHA256 = "rsa-sha256"

  def removeWhiteSpace(text: String) = text.replaceAll("""[  \t\n\r]""", "")


  def mapFields(fieldText: String): Map[String, String] = {

    val text = fieldText.trim
    text.split(';').filter(!_.trim.isEmpty).map{field: String =>

      val values =  field.toString.split("=", 2)
      if (values.length < 2)
        throw new Exception(s"Invalid header field, no = found in tag list field; text: $text ; field: $field" )

      val key = values(0).trim.toLowerCase
      val value = values(1).trim

      (key -> value)
    }.toMap
  }

  def getValue(map: Map[String, String], key: String, elseValue: String = ""): String = map.getOrElse(key, elseValue)

  def safeToLong(value: String, field: String): Long = {
    try{
      if (value.isEmpty)
        0
      else
        value.toLong
    } catch {
      case t: Throwable => throw new Exception(s"Improperly formatted number field: $field value: $value")
    }

  }

  def safeToInt(value: String, field: String): Int = {
    try {
      if (value.isEmpty)
        0
      else
        value.toInt
    } catch {
      case t: Throwable => throw new Exception(s"Improperly formatted number field: $field value: $value")
    }
  }

  def create(signature: String): DkimResult = {

    try{
      val dkim = rawCreate(signature)
      new DkimResult("", true, dkim)
    } catch {
      case t: Throwable => {
        logger.debug(s"invalid signature: ${t.getMessage}")
        new DkimResult(t.getMessage, false, DkimSignature(rawSignature = signature))
      };
    }
  }

  private def rawCreate(signature: String): DkimSignature = {
    val map = mapFields(signature)

    val v = getValue(map, "v")
    if (v != "1")
      throw new Exception(s"Unsupported version: $v")

    val a = getValue(map, "a").toLowerCase
    if (a != SHA1 && a != SHA256)
      throw new Exception(s"Invalid algorithm tag: $a")

    val b = removeWhiteSpace(getValue(map, "b"))
    val bh = removeWhiteSpace(getValue(map, "bh"))

    val d = getValue(map, "d")
    if (d.isEmpty)
      throw new Exception("d tag can not be empty")

    val h = getValue(map, "h")
    if (h.isEmpty)
      throw new Exception("h tag can not be empty")

    val s = getValue(map, "s")

    val t = safeToLong(getValue(map, "t"), "t (time)")
    val x = safeToLong(getValue(map, "x"), "x (expiration)")

    val c = getValue(map, "c", "simple/simple").toLowerCase
    val cvalues = getValue(map, "c").split("/")
    cvalues.foreach { value: String =>
      if (value != Simple && value != Relaxed)
        throw new Exception("Invalid canonicalization method $c")
    }

    val ch = cvalues(0)
    val cb = if (cvalues.length == 1) Simple else cvalues(1)

    val i = getValue(map, "i", "@sigDtag")
    val z = getValue(map, "z")

    val l = safeToInt(getValue(map, "l"), "l (body length)")

    new DkimSignature(rawSignature = signature, hashAlgorithm = a, signature = b, bodyHash = bh, domain = d, headers = h, dnsSelector = s,
      time = t, expiration = x, headerCanonicalization = ch, bodyCanonicalization = cb, identity = i,
      copiedHeaders = z, bodyLength = l)
  }

}

/**
 * Represents all of the information from a parsed DKIM header
 */
class DkimSignature (

   /**
    * dkim header text (without any processing
    */
    val rawSignature: String,

    /**
     * Hash algorithm. a tag.
     * This currently can be either:
     *   rsa-sha1 --> SHA1
     *   rsa-sha256 --> SHA-256
     * RFC REQUIRED
     */
    val hashAlgorithm: String,

    /**
     * Signature tag. b tag.
     * Contains the base64 encoded signature data. This is the signature we are verifying against.
     * RFC REQUIRED.
     */
    val signature: String,

    /**
     * Body Hash. bh tag.
     * The hash of the canonicalised body data.
     * RFC REQUIRED
     */
    val bodyHash: String,

    /**
     * Domain. d tag.
     * This is the domain which is signing the message.
     * RFC REQUIRED.
     */
    val domain: String,

    /**
     * Headers. h tag.
     * This is a list of headers, which should be signed and verified.  The verifier may
     * fail any message that does not sign a reasonable set of headers.
     * RFC REQUIRED.
     */
    val headers: String,

    /**
     * Selector. s tag.
     * What sub domain to use for querying DNS for the public key
     * RFC REQUIRED
     */
    val dnsSelector: String,

    /**
     * Time. t tag.
     * The time at which this message was signed.
     * Unix timestamp as string, no longer than 12 chars.
     * RFC RECOMMENDED (will default to 0 meaning unknown).
     */
    val time: Long,

    /**
     * Expiration. x tag.
     * The time at which the signature on this message expires.
     * Unix timestamp as string, no longer than 12 chars.
     * RFC RECOMMENDED (will default to 0 meaning no expiration).
     */
    val expiration: Long,

    /**
     * Header Canonicalization. c tag (first part).
     * RFC OPTIONAL (will default to "simple").
     */
    val headerCanonicalization : String,

    /**
      * Body Canonicalization. c tag (second part).
      * RFC OPTIONAL (will default to "simple").
      */
    val bodyCanonicalization : String,


    /**
     * Identity. i tag. This is the identity for which the mail is being signed.
     * RFC OPTIONAL (will default to "@sigDtag").
     */
    val identity: String,

    /**
     * Copied Headers. z tag. A copy of the some of the original headers, separated by the
     * pipe "|" symbol, and "|" in the headers/values must be converted to %x7C.
     * RFC OPTIONAL (will default to "" meaning no copied headers).
     */
    val copiedHeaders: String,

    /**
     * Body Length (used in signing). l tag.
     * RFC OPTIONAL (will default to 0 which means entire body).
     */
    val bodyLength: Int
) {

  import DkimSignature._

  def headerArray: Array[String] = headers.split("\\s*:\\s*").map{_.trim}
  def dnsRecord = dnsSelector + "._domainkey." + domain

  def getMethodDigestAlgorithm: String = {
    if (hashAlgorithm == SHA256)
      "SHA-256"
    else
      "SHA-1"
  }

  /**
   * Get the algorithm to use with java Signature class
   */
  def getSignatureAlgorithm: String = {
    if (hashAlgorithm == SHA256) {
      return "SHA256withRSA"
    }
    else {
      return "SHA1withRSA"
    }
  }

}





