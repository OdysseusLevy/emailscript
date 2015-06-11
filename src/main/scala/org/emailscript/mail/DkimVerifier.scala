package org.emailscript.mail

import java.io.{BufferedInputStream, ByteArrayInputStream, InputStream}
import java.security.{Signature,PublicKey}

import badpenguin.dkim.Verifier
import badpenguin.dkim.{CanonicalMethod, NSKey, DkimSignature, NSKeyStore}
import org.slf4j.LoggerFactory
import sun.misc.BASE64Decoder

/**
 * Experimental dkim verifier
 *
 * Currently only checks that there is a valid dkim host. No checks are run against the hashed data. I'm investigating
 * whether it is possible to verifyHost just the dkim encoded headers and skip the body verification (because that would be
 * much faster).
 *
 * Surprisingly, though, just the inclusion of a valid dkim header is a pretty good signal that the mail is not spam.
 *
 */
object DkimVerifier {

  val DkimHeader = "Dkim-Signature"

  val logger = LoggerFactory.getLogger(getClass)
  val keyStore = new NSKeyStore("dns", "8.8.8.8") //This is one of Google's DNS servers

  private def getPublicKey(signature: DkimSignature): Option[PublicKey] = {
    val nsKeys: Array[NSKey] = keyStore.retrieveKeys(signature.getDnsRecord)

    logger.info(s"numKeys: ${nsKeys.size}")
    if (nsKeys == null || nsKeys.length == 0)
      None
    else
      Option(nsKeys(nsKeys.length - 1).getKey)


  }

  def checkValidity(option: Option[DkimSignature]): Option[DkimSignature] = {

    try{
      option.map{signature =>
        signature.checkValidity()
        signature}
    } catch {
      case t: Throwable => logger.debug(s"invalid dkim header ${t.getMessage}"); None
    }
  }

  private def processHeader(header: String, value: String, method: CanonicalMethod): String = {

    if (method == CanonicalMethod.RELAXED) {
        processLine(header.toLowerCase().trim() + ":" + value.trim(), method)
    }else {
      processLine(header + ":" + value, method)
    }

  }

  private def processLine(input: String, method: CanonicalMethod): String = {
    var line = input

    if (method == CanonicalMethod.RELAXED) {
      line = line.replaceAll("[\r\n\t ]+", " ")
      line = line.replaceAll("(?m)[\t\r\n ]+$", "")
    }
    line
  }

  private def processDKimHeader(value: String): String = {

    value.replaceAll("b=[^;]*", "b=")
  }

  private def getDkimHeaders(message: MailMessageHelper, dkim: DkimSignature): String = {

    val headers: Array[String] = dkim.getHtag.split("\\s*:\\s*").map{_.trim}

    var result: String = ""
    headers.foreach{ header =>
      val headerValue = message.getHeader(header).getOrElse("")
      result += processHeader(header, headerValue, dkim.getHeaderMethod) + "\r\n"
    }

    val value = processDKimHeader(message.getHeader(DkimHeader).getOrElse(""))
    result + processHeader("DKIM-Signature", value, dkim.getHeaderMethod)
  }

  private def testDkim(message: MailMessageHelper): Unit = {

    val input = new ByteArrayInputStream(message.getBytes.toByteArray)

    val v = new Verifier(keyStore)

    v.verifyMail(input)

  }

  def verify(message: MailMessageHelper, dkim: DkimSignature, key: PublicKey): Option[String]= {

    testDkim(message)
    val headers = getDkimHeaders(message, dkim)
    logger.info(s"method: ${dkim.getHeaderMethod()} headers: $headers")
    logger.info(s"dkim: ${dkim}")

    val bs: BASE64Decoder = new BASE64Decoder
    val sigBuf: Array[Byte] = bs.decodeBuffer(dkim.getBtag)

    try {
      val sig = Signature.getInstance(dkim.getJavaAlg());
      sig.initVerify(key);
      sig.update(headers.getBytes());

      val result = sig.verify(sigBuf)
      logger.info(s"RESULT: $result")
      if (result)
        Option(dkim.getDtag)
      else
        None

    } catch {
      case t: Throwable => logger.info("verifyHost error", t); None
    }
  }

  /**
   * Determines what host sent this email.
   * We check this against the dns server, but do no further verification (ie. we don't do the hash verification)
   *
   * @param message
   * @return
   */
  def verifiedHost(message: MailMessageHelper):Option[String] = {
    val header = message.getHeader(DkimHeader)
    val signature:Option[DkimSignature] = header.map{text: String => new DkimSignature(DkimHeader + ":" + text, true)}

    for{ sig <- checkValidity(signature); key <- getPublicKey(sig); host <- verify(message, sig, key)}
      yield host

  }

}
