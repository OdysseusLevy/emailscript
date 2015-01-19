package org.emailscript.api

import java.net.URL
import java.util.Date

import org.emailscript.dnsbl.DnsblResult
import org.emailscript.mail.MailMessageHelper
import scala.collection.JavaConverters._

/**
 * This is an email returned from the server.
 *
 * ==Features==
 *  - Move to other folder
 *  - Delete (safe and permanent)
 *  - Check for spam links
 *  - Check for verified header (useful for spam detection)
 */
class MailMessage(helper: MailMessageHelper) {

  /**
   * Email subject
   */
  def getSubject(): String = helper.subject

  /**
   * Who the email was sent to
   */
  def getTo(): Array[Who] = helper.to

  /**
   * Carbon copy recipients
   */
  def getCc(): Array[Who] = helper.cc

  /**
   * Blind carbon copy recipients (other recipients don't see these recipients
   */
  def getBcc(): Array[Who] = helper.bcc

  /**
   * Who to reply to (assumes only one)
   */
  def getReplyTo():Who = helper.replyTo

  /**
   * Who sent this (assumes only one sender)
   */
  def getFrom():Who = helper.from

  /**
   * All people who sent this
   */
  def getFromAll(): Array[Who] = helper.fromAll

  /**
   * IsRead flag is set?
   */
  def getIsRead(): Boolean = helper.isRead

  /**
   * Universal ID
   */

  def getUid(): Long = helper.uid

  /**
   * Body Text
   * @note -- this is a slower operation
   */
  def getBody(): String = helper.body

  /**
   * Urls contained in body
   * @note -- this is a slower operation
   */
  def getUrl: Set[URL] = helper.urls

  /**
   * The first spam link found. Will be DnsblResult.empty if nothing found
   * @note this is a slower operation
   */
  def getSpamLink: DnsblResult = helper.spamLink

  /**
   * True if a spam link has been found
   */
  def getHasSpamLink(): Boolean = helper.hasSpamLink

  /**
   * Returns all the email headers
   * NOTE -- this is a slower operation
   */
  def getHeaders(): java.util.Map[String, String] = helper.headers.asJava

  /**
   * Message size in bytes
   */
  def getSize(): Int = helper.size

  /**
   * Is the host verified by the DKIM standars?
   * Very useful for detecting spam
   */
  def getIsVerifiedHost(): Boolean = helper.verifiedHost != "" //For groovy java bean
  //
  // Time stuff
  //

  /**
   * When was this email received
   */
  def getReceived(): Date = helper.received

  /**
   * How many weeks ago was this message received?
   */
  def getWeeksAgo(): Long = helper.weeksAgo

  /**
   * Howe may days ago was this message received?
   */
  def getDaysAgo(): Long = helper.daysAgo

  /**
   * How many hours ago was this message received
   */
  def getHoursAgo(): Long = helper.hoursAgo

  //
  // Headers
  //

  /**
   * If this email was moved to a folder using Emailscript, a special header is added
   * You can use this property to what folder this email was moved from
   */
  def getMoveHeader(): String = helper.moveHeader.getOrElse("")

  def hasHeader(name: String): Boolean = helper.hasHeader(name)
  def getHeader(name: String): Option[String] = helper.getHeader(name)

  //
  // Commands
  //

  /**
   * Check if this email was sent to the given email
   * @param email
   */
  def wasSentTo(email: String): Boolean = helper.sentTo(email)

  /**
   * Move email to the given folder
   * @param folderName
   */
  def moveTo(folderName: String) = helper.moveTo(folderName)

  /**
   * Delete this email, if possible will move to "Trash" folder instead of permanently deleting it
   */
  def delete() = helper.delete()

  /**
   * Delete this email.
   * @param permanent if true, permanently deleted, otherwise the email is moved to the "Trash" folder
   */
  def delete(permanent: Boolean): Unit = helper.delete(permanent)
}

object MailMessage {
  def apply(helper: MailMessageHelper) = new MailMessage(helper)
}