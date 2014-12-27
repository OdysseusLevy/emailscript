package org.emailscript

import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.Date
import javax.mail.Message.RecipientType
import javax.mail._

import com.sun.mail.imap.IMAPMessage
import org.emailscript.beans.Who
import org.emailscript.url._
import org.slf4j.LoggerFactory

object MailMessage {

  val logger = LoggerFactory.getLogger(getClass)
  val millisInHour: Long = 60 * 60 * 1000
  val millisInDay: Long = 24 * millisInHour
  val millisInWeek: Long = 7 * millisInDay

  val DefaultDnsblLookups = new MultipleDnsblLookup(Seq(SurblDnsbl(), SpamHausDnsbl()))
  def getOrElse[A](value: A, default: A): A = if (value == null) default else value



  def getHeaders(message: Part): Map[String,String] = {
    var map = Map[String,String]()
    val enum = message.getAllHeaders
    while(enum.hasMoreElements) {
      val header = enum.nextElement().asInstanceOf[Header]
      map = map + (header.getName.toLowerCase -> header.getValue)
    }

    map
  }
}

class MailMessage(val message: IMAPMessage, dnsbl: DnsblLookup = MailMessage.DefaultDnsblLookups) {

  import org.emailscript.MailMessage._
  message.setPeek(true)

  lazy val subject: String = getOrElse(message.getSubject, "")

  lazy val to: Array[Who] = Who.getAll(message.getRecipients(RecipientType.TO))
  lazy val cc: Array[Who] = Who.getAll(message.getRecipients(RecipientType.CC))
  lazy val bcc: Array[Who] = Who.getAll(message.getRecipients(RecipientType.BCC))

  /**
   * Simplify by only allowing one replyTo or from
   */
  lazy val replyTo: Who = Who.getOne(message.getReplyTo)
  lazy val from: Who = Who.getOne(message.getFrom)

  lazy val isRead = message.isSet(Flags.Flag.SEEN)
  lazy val uid: Long = MailUtils.getUID(message.getFolder, message)
  lazy val body: String = MailUtils.getBodyText(message).getOrElse("")
  lazy val urls: Set[URL] = SpamUrlParser.findUrls(body)
  lazy val spamLink: DnsblResult = SpamUrlParser.findSpamLink(urls, dnsbl)
  lazy val hasSpamLink: Boolean = spamLink != DnsblResult.empty
  lazy val headers = getHeaders(message)
  lazy val size = message.getSize
  lazy val verifiedHost = DkimVerifier.verifiedHost(this).getOrElse("")
  lazy val moveHeader: String = getHeader(MailUtils.MoveHeader).getOrElse("")

  def dumpStructure = MailUtils.dumpStructure(message)

  //
  // Time stuff
  //

  //TODO redo these using Java 1.8 date utils

  lazy val received: Date = message.getReceivedDate
  lazy val ageInMillis = System.currentTimeMillis() - received.getTime

  lazy val weeksAgo: Long = ageInMillis / millisInWeek
  lazy val daysAgo: Long =  ageInMillis / millisInDay
  lazy val hoursAgo: Long = ageInMillis / millisInHour

  //
  // Headers
  //

  def hasHeader(name: String): Boolean = headers.contains(name.toLowerCase)
  def getHeader(name: String): Option[String] = headers.get(name.toLowerCase)

  def sentTo(email: String): Boolean = to.exists{ def emailLower = email.toLowerCase; contact => contact.email == emailLower}

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

}
