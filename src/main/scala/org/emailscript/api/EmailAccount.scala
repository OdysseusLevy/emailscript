package org.emailscript.api

import java.util.{Date, Properties}

import org.emailscript.helpers.Importer
import org.emailscript.mail.MailUtils

import scala.beans.BeanProperty

/**
 * Used to configure an EmailAccount
 */
class EmailAccountBean extends NamedBean with Importer{

  def doImport(): AnyRef = {
    EmailAccount(this)
  }
  /**
   * Server used to receive mail (using the IMAP protocol). For example gmail uses: imap.gmail.com
   * @group Properties
   */
  @BeanProperty var imapHost: String = ""

  /**
   * Optional, Advanced property -- usually the default will work
   * @group Properties
   */
  @BeanProperty var imapPort: Int = -1

  /**
   * Set user name. For example a gmail user name would be something like myname@gmail.com
   * @group Properties
   */
  @BeanProperty var user: String = ""

  /**
   * Password (assumed to work with both imap and smtp)
   * @group Properties
   */
  @BeanProperty var password: String = ""

  /**
   * Server used to send out email. For example gmail uses smtp.gmail.com
   * @group Properties
   */
  @BeanProperty var smtpHost: String = ""
  @BeanProperty var smtpPort: Int = 465
}

/**
 * ==Features==
 *
 * Things you can do:
 *  - Get some or all emails from a folder
 *  - Get only the latest emails from a folder
 *  - Scan a folder and run a callback each time new mail appears
 *  - Send mail
 *
 * ==Configuration==
 *
 * When running from the command line you set up access to your email account by adding a .yml file to the config
 * directory
 * {{{
 * !EmailAccount
 * imapHost: <ImapHost>
 * user: <User@MyDomain>
 * password: <Password>
 * smtpHost: <SmtpHost>
 * }}}
 *
 */
class EmailAccount(val user: String, val password: String, val imapHost: String, val imapPort: Int,
                   val smtpHost: String, val smtpPort: Int) {

  import EmailAccount._

  /**
   * Create an email object that can be sent
   * @group Functions
   */
  def newMail() = EmailBean(user)

  /**
   * Send an email
   * @param message email to send
   * @group Functions
   */
  def send(message: EmailBean) = MailUtils.sendMessage(this, message)

  /**
   * Continuously scan the folder waiting for new messages to appear.
   * The first time this is run the entire contents of the folder will be scanned. After that the callback will be
   * triggered whenever new messages are added to the folder
   *
   * @param folderName folder to Scan
   * @param scanner callback
   * @group Functions
   */
  def scanFolder(folderName: String, scanner: ProcessCallback): Unit = MailUtils.scanFolder(this, folderName, scanner)

  /**
   * Scan folder (same as above), but with option to not do the first, initial read
   *
   * @param folderName folder to scan
   * @param doFirstRead if true will first read unread messages
   * @param scanner user supplied callback to handle the emails
   */
  def scanFolder(folderName: String, doFirstRead: Boolean,  scanner: ProcessCallback): Unit =
    MailUtils.scanFolder(this, folderName, scanner, doFirstRead)

  /**
   * Read in all new messages. The first time this is run it will read in all messages. Subsequent calls will only return
   * messages that have been added after the first time messages were read.
   * @param folderName folder to read from
   * @param callback user supplied callback to handle the emails
   * @group Functions
   */
  def readLatest(folderName: String, callback: ProcessCallback): Unit = MailUtils.readLatest(this, folderName, callback)

  def foreach(emails: Array[Email], script: ProcessCallback): Unit = {
    emails.foreach(script.callback(_))
  }

  /**
   * Runs a script against all inbox emails
   * @group Functions
   */
  def foreach(script: ProcessCallback): Unit = foreach(getEmails(), script)

  /**
   * Runs a script against a list of emails with the given uid
   *
   * @param ids array of specific uid's of the desired emails
   * @param script user callback
   * @group Functions
   */
  def foreach(ids: java.util.ArrayList[Number], script:ProcessCallback): Unit = foreach(getEmails(ids), script)

  /**
   * Runs a script against all of a given folder
   *
   * @param folderName folder to read in
   *  @group Functions
   */
  def foreach(folderName: String, script: ProcessCallback): Unit = foreach(getEmails(folderName), script)

  /**
   * Runs a script against a given folder and limit of how many
   * @param folderName folder to read (eg. Inbox, Junk, etc.)
   * @param limit returns newest emails up to this limit
   * @param script
   *  @group Functions
   */
  def foreach(folderName: String, limit: Int, script: ProcessCallback): Unit = foreach(getEmails(folderName, limit), script)

  /**
   * Runs a script against the Inbox with a given limit of how many email to read
   *
   * @param limit returns newest emails up to this limit
   * @param script
   *  @group Functions
   */
  def foreach(limit: Int, script: ProcessCallback): Unit = foreach(getEmails(Inbox, limit), script)

  /**
   * Read all messages from Inbox
   * @group Functions
   */
  def getEmails(): Array[Email] = getEmails(Inbox, 0)

  /**
   * Get emails from Inbox
   * @param limit use this to limit how many messages. Eg. a limit of 10 will return the latest 10 messages. 0 returns all messages
   * @group Functions
   */
  def getEmails(limit: Int): Array[Email] = getEmails(Inbox, limit)

  /**
   * Get specific emails by id
   * @param ids uid's for the desired mails
   */
  def getEmails(ids: java.util.ArrayList[Number]): Array[Email] = getEmails(Inbox, ids)


  /**
   * Return all emails before a given date
   * @param folder folder we are reading
   * @param date return all emails that are older than this date
   * @group Functions
   */
  def getEmailsBefore(folder: String, date: java.util.Date) = MailUtils.getEmailsBefore(this, folder, date)

  /**
   * Get all emails after a given date
   * @param folder folder we are reading
   * @param date return all emails that are newer than this date
   *@group Functions
   */
  def getEmailsAfter(folder: String, date: Date) = MailUtils.getEmailsAfter(this, folder, date)

  /**
   * Get all emails from a specified folder
   * @param folderName
   * @group Functions
   */
  def getEmails(folderName: String): Array[Email] = getEmails(folderName, 0)

  /**
   * Get emails from a specified folder with a specified limit
   *
   * @param folderName
   * @param limit
   * @group Functions
   */
  def getEmails(folderName: String, limit: Int): Array[Email] = MailUtils.getEmails(this, folderName, limit)

  def getEmails(folderName: String, ids: java.util.ArrayList[Number]) = MailUtils.getEmails(this, folderName, ids)

  /**
   *
   * @param folderName
   * @param startUID
   * @group Functions
   */
  def getEmailsAfter(folderName: String, startUID: java.lang.Long): Array[Email] = {
    MailUtils.getEmailsAfter(this, folderName, startUID)
  }

  /**
   * Returns true if this is a gmail account
   *
   * @group Functions
   */
  def isGmail() = user.endsWith("gmail.com")

  //
  // Folder support
  //

  /**
   * Returns true if folder exists, false otherwise
   * @param folderName
   * @group Functions
   */
  def hasFolder(folderName : String): Boolean = MailUtils.hasFolder(this, folderName)

  /**
   * Return all of the folder names for this account
   * @group Functions
   */
  def getFolders(): Array[String] = MailUtils.getFolderNames(this)

}

object EmailAccount {

  val Inbox = "Inbox"

  private val Trash = "Trash"
  private val GmailTrash = "[Gmail]/Trash"

  def trashFolder(account: EmailAccount): String = {
    if (account.isGmail())
      GmailTrash
    else
      Trash
  }

  val gmailTopLevelFolders = Set("inbox", "deleted messages", "drafts", "sent", "sent messages")

  def getFolderName(account: EmailAccount, name: String): String = {
    if (!account.isGmail)
      return name

    if (name == Trash)
      return GmailTrash

    if (name == GmailTrash)
      return name

    if (name.startsWith("[") || gmailTopLevelFolders.contains(name.toLowerCase))
      return name

    "[Gmail]/" + name
  }

  def apply(bean: EmailAccountBean) = {
    new EmailAccount(bean.getUser, bean.getPassword, bean.getImapHost, bean.getImapPort, bean.getSmtpHost, bean.getSmtpPort)
  }

  def createBean(imapHost: String, user: String, password: String, smtpHost: String): EmailAccountBean = {
    val bean = new EmailAccountBean
    bean.setImapHost(imapHost)
    bean.setUser(user)
    bean.setPassword(password)
    bean.setSmtpHost(smtpHost)

    bean
  }

  def toSmtpProperties(account: EmailAccount):Properties = {
    val props = new Properties()
    props.put("mail.smtp.host", account.smtpHost)
    props.put("mail.smtp.socketFactory.port", account.smtpPort.toString)
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.port", account.smtpPort.toString)
    props
  }
}
