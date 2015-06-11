package org.emailscript.mail

import java.io.{File, ByteArrayOutputStream}
import java.net.URL
import java.time.{Duration, Instant}
import javax.mail.Message.RecipientType
import javax.mail._

import com.sun.mail.imap.IMAPMessage
import org.emailscript.api.{EmailAccount, Who}
import org.emailscript.dnsbl._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object MailMessageHelper {

  val logger = LoggerFactory.getLogger(getClass)

  val DefaultDnsblLookups = new MultipleDnsblLookup(Seq(SurblDnsbl(), SpamHausDnsbl()))
  def getOrElse[A](value: A, default: A): A = if (value == null) default else value

  def fetchOneHeader(message: Part, header: String): Array[String] = {
    message.getHeader(header)
  }

  def getAllHeaders(message: Part): Iterator[(String, String)] = {
    message.getAllHeaders.asScala.map { case header: Header =>
      (header.getName.toLowerCase -> header.getValue)
    }
  }

  def getHeaders(message: Part): Map[String,String] = {
    getAllHeaders(message).toMap
  }
}

class MailMessageHelper(val account: EmailAccount, val message: IMAPMessage, dnsbl: DnsblLookup = MailMessageHelper.DefaultDnsblLookups) {
  message.setPeek(true)

  lazy val subject: String = MailMessageHelper.getOrElse(message.getSubject, "")

  lazy val to: Array[Who] = Who.getAll(message.getRecipients(RecipientType.TO))
  lazy val cc: Array[Who] = Who.getAll(message.getRecipients(RecipientType.CC))
  lazy val bcc: Array[Who] = Who.getAll(message.getRecipients(RecipientType.BCC))

  // Simplify by only allowing one replyTo or from
  lazy val replyTo: Who = Who.getOne(message.getReplyTo)
  lazy val replyToAll: Array[Who] = Who.getAll(message.getReplyTo)

  lazy val from: Who = Who.getOne(message.getFrom)
  lazy val fromAll: Array[Who] = Who.getAll(message.getFrom)

  lazy val folder = message.getFolder.getName
  lazy val isRead = message.isSet(Flags.Flag.SEEN)
  lazy val uid: Long = MailUtils.getUID(message.getFolder, message)
  lazy val body: String = MailUtils.getBodyText(message).getOrElse("")
  lazy val urls: Set[URL] = SpamUrlParser.findUrls(body)
  lazy val spamLink: DnsblResult = SpamUrlParser.findSpamLink(urls, dnsbl)
  lazy val hasSpamLink: Boolean = spamLink != DnsblResult.empty
  lazy val headers = MailMessageHelper.getHeaders(message)
  lazy val size = message.getSize
  lazy val dkimResult = dkim.DkimVerifier.verify(this.message)
  lazy val dkimHeader = dkim.DkimVerifier.verifyHeaders(this.message)
  lazy val verifiedHost = if (dkimHeader.isDefined) dkimHeader.get.domain else ""
  lazy val moveHeader: Option[String] = {

    val result = MailMessageHelper.fetchOneHeader(message, MailUtils.MoveHeader)
    if (result == null || result.size == 0)
      None
    else
      Option(result(0))
  }

  def dumpStructure = MailUtils.dumpStructure(message)

  //
  // Time stuff
  //

  lazy val received = message.getReceivedDate
  lazy val messageAge = Duration.between(received.toInstant, Instant.now())

  lazy val weeksAgo: Long = messageAge.toDays / 7
  lazy val daysAgo: Long =  messageAge.toDays
  lazy val hoursAgo: Long = messageAge.toHours

  //
  // Headers
  //

  def hasHeader(name: String): Boolean = headers.contains(name.toLowerCase)
  def getHeader(name: String): Option[String] = headers.get(name.toLowerCase)
  def sentTo(email: String): Boolean = {
    val emailLower = email.toLowerCase
    to.exists{ _.getEmail == emailLower}
  }

  //
  // Commands
  //

  def moveTo(folderName: String) {
    MailUtils.moveTo(folderName, this)
  }

  def getBytes : ByteArrayOutputStream = {
    val os =  new ByteArrayOutputStream()
    val bis = message.writeTo(os)
    os
  }

  def delete() {
    MailUtils.delete(false, this)
  }

  def delete(permanent: Boolean): Unit = {
    MailUtils.delete(permanent, this)
  }

  def saveToFile(fileName: String): Unit = MailUtils.saveToFile(message, new File(fileName))
}
