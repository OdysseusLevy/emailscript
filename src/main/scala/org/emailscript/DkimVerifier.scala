package org.emailscript

import badpenguin.dkim.{DkimSignature, NSKeyStore}
import org.slf4j.LoggerFactory
import org.xbill.DNS.{Lookup, SimpleResolver, Type}

/**
 * Experimental dkim verifier
 *
 * Currently only checks that there is a valid dkim host. No checks are run against the hashed data. I'm investigating
 * whether it is possible to verify just the dkim encoded headers and skip the body verification (because that would be
 * much faster).
 *
 * Surprisingly, though, just the inclusion of a valid dkim header is a pretty good signal that the mail is not spam.
 *
 */
object DkimVerifier {

  val DkimHeader = "Dkim-Signature"

  val logger = LoggerFactory.getLogger(getClass)
  val keyStore = new NSKeyStore("dns", "8.8.8.8")

  private def getDnsHeaders(signature: DkimSignature): Option[String] = {
    val lookup = new Lookup(signature.getDnsRecord, Type.TXT)
    lookup.setResolver(new SimpleResolver())
    val records = lookup.run()
    if (records == null || records.size == 0)
      None
    else
      Some(records(0).toString)
  }

  /**
   * Determines what host sent this email.
   * We check this against the dns server, but do no further verification (ie. we don't do the hash verification)
   *
   * @param message
   * @return
   */
  def verifiedHost(message: MailMessage):Option[String] = {
    val header = message.getHeader(DkimHeader)
    val signature:Option[DkimSignature] = header.map{text: String => new DkimSignature(DkimHeader + ":" + text, true)}
    val dnsHeaders =  signature.flatMap(getDnsHeaders(_))

    if (dnsHeaders.isDefined)
      Option(signature.get.getDtag)
    else
      None
  }

}
