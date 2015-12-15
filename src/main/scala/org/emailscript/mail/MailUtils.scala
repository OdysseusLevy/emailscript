package org.emailscript.mail

import java.io.{File, FileOutputStream}
import java.util
import java.util.{Date, Properties}
import javax.mail.Flags.Flag
import javax.mail.{Folder => JavaMailFolder, _}
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.search.{ComparisonTerm, ReceivedDateTerm}

import com.sun.mail.imap.{IMAPFolder, IMAPMessage}
import com.sun.mail.pop3.POP3Folder
import org.emailscript.api._
import org.emailscript.helpers.{Configuration, LoggerFactory, Tags, Values, Yaml}


/**
 * Basic mail handling utilities using javax.mail
 */
object MailUtils {

  val yaml = Yaml(Configuration.DataDir)

  val logger = LoggerFactory.getLogger(getClass)

  val MoveHeader = "Mailscript-Move"

  var dryRun: Boolean = false
  lazy val defaultPermissions = if (dryRun) JavaMailFolder.READ_ONLY else JavaMailFolder.READ_WRITE


  def sendMessage(account: EmailAccountBean, messageBean: EmailBean) = {

    logger.info(s"sending email to ${messageBean.getTo}")
    val session = Session.getInstance(toSmtpProperties(account))
    val message = messageBean.toMessage(session)
    Transport.send(message, account.user, account.password)
  }


  def moveTo(toFolderName: String, m: MailMessageHelper) {

    if (dryRun) {
      logger.info(s"DRY RUN -- moving message from: ${m.from} subject: ${m.subject} to folder: $toFolderName")
      return
    }

    val fromFolder: JavaMailFolder = m.message.getFolder
    val store = fromFolder.getStore
    val toFolder = store.getFolder(toFolderName).asInstanceOf[IMAPFolder]
    if (!toFolder.exists()){
      logger.warn(s"ignoring request to move message to folder that does not exist: $toFolderName")
      return
    }

    try {
      toFolder.open(JavaMailFolder.READ_WRITE)

      val newMessage = new MimeMessage(m.message.asInstanceOf[MimeMessage])

      newMessage.removeHeader(MoveHeader)
      newMessage.addHeader(MoveHeader, toFolderName)

      val messageArray: Array[Message] = Array(newMessage)
      logger.info(s"moving mail from: ${m.from} subject: ${m.subject} to folder: $toFolderName")
      toFolder.appendMessages(messageArray)
      m.message.setFlag(Flag.DELETED, true)
    } catch {
      case e: Throwable => logger.warn(s"failed moving message to folder: $toFolderName", e)
    }

    closeFolder(toFolder, true)
  }

  def delete(permanent: Boolean, m: MailMessageHelper): Unit = {

    if (dryRun) {
      logger.info(s"DRY RUN -- deleting message from: ${m.from} subject: ${m.subject}")
      return
    }

    if (permanent)
      m.message.setFlag(Flag.DELETED, true)
    else
      moveTo(trashFolder(m.account), m)
  }

  def closeFolder(folder: IMAPFolder, expunge: Boolean = !dryRun): Unit = {
    try {
      if (folder != null && folder.isOpen)
        folder.close(expunge)
    } catch {
      case e: Throwable => //ignore
    }
  }

  def saveToFile(message: IMAPMessage, file : File) = {
    message.writeTo(new FileOutputStream(file))
  }

  def getUID(folder: JavaMailFolder, m: Message): Long = {
    folder match {
      case imap: IMAPFolder => imap.getUID(m)
      case pop3: POP3Folder => pop3.getUID(m).toLong
    }
  }

  def isValidEmail(email: String): Boolean = {
    try {
      val emailAddr = new InternetAddress(email)
      emailAddr.validate()
      true
    } catch {
      case e: Throwable => false
    }
  }

  def toSmtpProperties(account: EmailAccountBean):Properties = {
    val props = new Properties()
    props.put("mail.smtp.host", account.smtpHost)
    props.put("mail.smtp.socketFactory.port", account.smtpPort.toString)
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.port", account.smtpPort.toString)
    props
  }

  def createDataName(account: EmailAccountBean, dataName: String, folderName: String) = {
    s"${account.user}-${folderName}$dataName"
  }

  //
  // Folder names
  //

  private val Trash = "Trash"
  private val GmailTrash = "[Gmail]/Trash"

  private val Spam = "Spam"
  private val GmailSpam = "[Gmail]/Spam"

  def trashFolder(account: EmailAccountBean): String = {
    if (isGmail(account))
      GmailTrash
    else
      Trash
  }

  val gmailTopLevelFolders = Set("inbox", "deleted messages", "drafts", "sent", "sent messages")

  def isGmail(account: EmailAccountBean) = account.user.toLowerCase.endsWith("gmail.com")

  def getFolderName(account: EmailAccountBean, name: String): String = {
    if (!isGmail(account))
      return name

    // Handle Gmail specific folders

    name.toLowerCase() match {
      case "trash" => "[Gmail]/Trash"
      case "spam" => "[Gmail]/Spam"
      case "drafts" => "[Gmail]/Drafts"
      case _ => name
    }
  }

}
