package org.emailscript.dkim

import java.io.InputStream
import java.security.{MessageDigest, Signature}
import javax.mail.internet.MimeMessage

import org.emailscript.dkim.DkimVerifier.HeaderStack
import org.emailscript.helpers.{DnsHelper, LoggerFactory}
import sun.misc.{BASE64Decoder, BASE64Encoder}

import scala.collection.JavaConverters._
import scala.collection.mutable

case class Body(bytes: Array[Byte], length: Int)

// Want this to mimic an Option that has extra information
case class DkimResult(description: String,           // Error message, if any goes here
                      valid: Boolean,
                      dkim: DkimSignature = DkimSignature.empty) {  // Dkim Signature record goes here on success

  def isDefined() = valid
  def get = dkim
}

case class Header(key: String, value: String, line: String) {

  val keyl = key.trim.toLowerCase
}

object DkimVerifier {

  case class Body(bytes: Array[Byte], length: Int)

  import DkimSignature._

  val logger = LoggerFactory.getLogger(getClass)

  type HeaderStack = Map[String, mutable.Stack[Header]]

  /**
   * Do full DKIM verification of both the email headers and body
   *
   * @param email JavaMail email
   * @param dnsHelper (optional) This enables us to query DNS records
   */
  def verify(email: MimeMessage, dnsHelper: DnsHelper = new DnsHelper): DkimResult = {

    val headerResult = verifyHeaders(email, dnsHelper)

    if (!headerResult.isDefined())
      return headerResult

    if (verifyBody(email, headerResult.dkim))
      headerResult
    else
      new DkimResult("Body hash did not match", false, headerResult.dkim)
  }

  /**
   * Verify just the headers -- don't do the body yet
   */
  def verifyHeaders(email: MimeMessage, dnsHelper: DnsHelper = new DnsHelper): DkimResult = {
    val headers = email.getAllHeaderLines.asScala.asInstanceOf[Iterator[String]]
    verifyHeaders(headers, dnsHelper)
  }

  def verifyHeaders(headers: Iterator[String], dnsHelper: DnsHelper): DkimResult = {

    val headerStacks = buildHeaderStacks(headers)
    val dkim = getDkim(headerStacks)
    if (dkim.isEmpty)
      return new DkimResult("No valid dkim header found", false)

    val verifier = new DkimVerifier(dkim.get,headerStacks, dnsHelper)
    verifier.verifyHeaders()
  }


  def hasCRLF(bytes: Array[Byte], index: Int) = {
    if (index + 1 > bytes.length || index < 0)
      false
    else
      bytes(index) == '\r' && bytes(index + 1) == '\n'
  }

  def simpleBodyFormat(is: InputStream, buffer: Array[Byte], length: Int): Int = {

    var byte: Int = is.read()
    var count = 0

    while ( (count < length) && (byte != -1)) {
      buffer(count) = byte.toByte
      count += 1
      byte = is.read()
    }

    count
  }

  def verifyBody(email: MimeMessage, dkim: DkimSignature): Boolean = {

    var length: Int = if (dkim.bodyLength > 0) dkim.bodyLength else email.getSize

    val body = new Array[Byte](length + 2)

    //
    // Pull in the body, using a single pass
    //

    if (dkim.bodyCanonicalization == DkimSignature.Relaxed)
      length = relaxedBodyFormat(email.getRawInputStream, body, length)
    else
      length = simpleBodyFormat(email.getRawInputStream, body, length)

    // Ensure that the body has a CRLF at the end

    if(!hasCRLF(body, length -2)){
      body(length) = '\r'
      body(length+1) = '\n'
      length += 2
    }

    // Remove any duplicate CRLF's at the end

    length = ignoreExtraEndLines(body, length)


    // Create hash and compare it

    val digest = getHash(body, length, dkim.getMethodDigestAlgorithm)

    digest == dkim.bodyHash
  }

  def ignoreExtraEndLines(bytes: Array[Byte], end: Int): Int = {
    var i = end
    while ( hasCRLF(bytes, i -4)) {
      i -= 2
    }

    i
  }

  def getHash(bytes: Array[Byte], length: Int, method: String): String = {
    val md: MessageDigest = MessageDigest.getInstance(method)
    md.update(bytes, 0, length)

    val bsenc: BASE64Encoder = new BASE64Encoder
    bsenc.encode(md.digest)
  }

  def removeTab(ch: Int) = if (ch == '\t') ' '.toInt else ch


  /**
   * Put the message body into relaxed format.
   *
   * Message bodies can be potentially quite large so for efficiency we want to do this running through
   * the body just one time
   */
  def relaxedBodyFormat(is: InputStream,  bytes: Array[Byte], length: Int): Int = {

    var outCount = 0
    var current = removeTab(is.read())

    while(current >= 0 && outCount < length){

      val next = removeTab(is.read())

      if (current != ' ' || !(next == ' ' || next == '\r')){
        bytes(outCount) = current.toByte
        outCount += 1
      }

      current = next
    }

    outCount
  }

  /**
   * An email might have multiple dkim signatures. For example, it is possible in the future
   * that an email will be signed with multiple dkim versions
   *
   * Return the first dkim signature that we recognize as valid
   */
  def getDkim(headerStacks: HeaderStack): Option[DkimSignature] = {

    val stack = headerStacks.get(DkimHeader.toLowerCase)
    if (stack.isEmpty)
      return None

    val headers = stack.get.toIterable.map{header => DkimSignature.create(header.value)}
    headers.find(dimResult => dimResult.isDefined()).map(_.get)
  }

  /**
   * Handle duplicate header values using a stack (as called for by the DKIM RFC)
   */
  def buildHeaderStacks(headers: Iterator[String]): HeaderStack= {

    var map = Map[String, mutable.Stack[Header]]()

    for( line <- headers; header <- parseHeader(line)) {

      if (!map.contains(header.keyl)) {
        val stack = new mutable.Stack[Header]
        stack.push(header)
        map += (header.keyl -> stack)
      } else {
        val stack = map.get(header.keyl).get
        stack.push(header)
      }
    }

    map
  }

  private def parseHeader(text : String): Option[Header] = {

    if (text == null || text.isEmpty)
      return None

    val parts = text.split(":", 2)
    if (parts.length != 2 || parts(0).isEmpty)
      return None

    val header = Header(parts(0), parts(1), text)

    Some(header)
  }

  /** DKIM RFC relax header canonicalization
    * NOTE: For simplicity, we just always remove CRLF's rather than do the "unfolding" that might possibly leave some
    * CRLF's intact
    */
  def relaxedHeaderFormat(input: String): String = {

    val line = input.replaceAll("[\r\n\t ]+", " ")
    line.replaceAll("(?m)[ ]+$", "") // remove all whitespace at end of line
  }

}

/**
 * Verify an email dkim signature
 *
 * Code is based on RFC 6376
 * https://tools.ietf.org/html/rfc6376
 *
 * comments about the "rfc" are referring to this RFC 6376
 *
 * @param headerStacks header, with multiple headers with the same key stored in stacks
 * @param dnsHelper used to get the public key from the dns host specified in the dkim header
 *
 */
class DkimVerifier(dkim: DkimSignature, headerStacks: HeaderStack, dnsHelper: DnsHelper = new DnsHelper) {
  import DkimSignature.{DkimHeader, Relaxed}
  import DkimVerifier._

  def verifyHeaders(): DkimResult = {

    try {
      val dnsLookup = new DkimDnsLookup(dnsHelper)

      val publicKey = dnsLookup.getPublicKey(dkim.dnsRecord)
      val dkimHeaderText = getSignatureText()

      logger.debug(s"headerText:\n$dkimHeaderText")

      val bs: BASE64Decoder = new BASE64Decoder
      val sigBuf: Array[Byte] = bs.decodeBuffer(dkim.signature)


      val sig = Signature.getInstance(dkim.getSignatureAlgorithm)
      sig.initVerify(publicKey)
      sig.update(dkimHeaderText.getBytes())

      val result = sig.verify(sigBuf)
      logger.debug(s"RESULT: $result domain: ${dkim.domain}")
      if (result)
        new DkimResult("Success", true, dkim)
      else{
        logger.debug(s"signature: ${dkim.rawSignature}")
        new DkimResult("Headers signature did not validate", false, dkim)
      }
    } catch {
      case t: Throwable => logger.debug("verifyHost error", t); new DkimResult(t.getMessage, false, dkim)
    }
  }

  /**
   * Produce the header text as specified by the rfc
   */
  def getSignatureText(): String = {

    val headers = dkim.headerArray

    // Add each requested header to our text

    var result: String = ""
    headers.foreach{ headerName =>

      val header = headerStacks.get(headerName.toLowerCase).flatMap(safePop)
      if (header.isDefined)
        result += processHeader(header.get, dkim.headerCanonicalization) + "\r\n"
    }

    // Add the dkim header at the end (we have to remove the signature field (b=...) )

    val dkimRaw = getHeader(DkimHeader)
    val dkimHeader = Header(dkimRaw.key, processDKimHeader(dkimRaw.value), processDKimHeader(dkimRaw.line))

    result + processHeader(dkimHeader, dkim.headerCanonicalization)
  }

  /**
   * When verifying, we need the original dkim signature, but with the b tag nulled out
   */
  private def processDKimHeader(text: String): String = {
    text.replaceAll("b=[^;]*", "b=")
  }

  private def safePop(stack: mutable.Stack[Header]): Option[Header] = {
    if (stack.isEmpty) None else Some(stack.pop())
  }

  private def getHeader(header: String): Header = {

    val result = headerStacks.get(header.toLowerCase).flatMap(safePop)
    result.getOrElse(Header(header, "", header + ":"))
  }

  private def processHeader(header: Header, method: String): String = {

    if (method == Relaxed) {
      relaxedHeaderFormat(header.keyl + ":" + header.value.trim())
    }else {
      header.line
    }
  }

}
