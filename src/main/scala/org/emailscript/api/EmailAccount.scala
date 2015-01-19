package org.emailscript.api

import java.util.{Date, Properties}
import org.emailscript.mail.{MailUtils}

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
 * When running from the command line you set up access to your email account by adding a .yml file to the accounts
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
class EmailAccount extends NamedBean with ValuesImmutableBean {

  //
  // Bean interface
  //

  /**
   * Set imap server address. For example gmail uses: imap.gmail.com
   * @group Properties
   */
  def setImapHost(imapHost: String): Unit = set('ImapHost, imapHost)

  /**
   * Get imap server address
   * @group Properties
   */
  def getImapHost(): String = getOrElse('ImapHost, "")

  /**
   * Set user name. For example a gmail user name would be something like myname@gmail.com
   * @group Properties
   */
  def setUser(user: String): Unit = set('User, user)

  /**
   * Get user name
   *  @group Properties
   */
  def getUser: String = getOrElse('User, "")

  /**
   * Set password
   * @group Properties
   */
  def setPassword(password: String): Unit = set('Password, password)

  /**
   * Get password
   * @group Properties
   */
  def getPassword(): String = getOrElse('Password, "")
  
  /**
   * Optional, Advanced property -- usually the default will work
   * @group Properties
   */
  def setImapPort(port: Int): Unit = set('ImapPort, port)

  /**
   * Optional, Advanced property -- usually the default will work
   * @group Properties */
  def getImapPort(): Int = getOrElse('ImapPort, -1)

  /**
   * Smtp server address. For example gmail uses smtp.gmail.com
   * @group Properties
   */
  def setSmtpHost(host: String): Unit = set('SmtpHost, host)

  /**
   * Smtp server address.
   * @group Properties*/
  def getSmtpHost(): String = getOrElse('SmtpHost, "")

  /**
   * [Optional] Advanced property -- usually the default will work
   * @group Properties
   */
  def setSmtpPort(port: Int): Unit = set('SmtpPort, port)

  /**
   * [Optional] Advanced property -- usually the default will work
   * @group Properties */
  def getSmtpPort(): Int = getOrElse('SmtpPort, 465)


  //
  // Methods
  //

  /**
   * Create an email object that can be sent
   * @group Functions
   */
  def newMail() = EmailBean(getUser)

  /**
   * Send an email
   * @param message
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
  def scanFolder(folderName: String, scanner: ScriptCallback): Unit = MailUtils.scanFolder(this, folderName, scanner)

  /**
   * Read in all new messages. The first time this is run it will read in all messages. Subsequent calls will only return
   * messages that have been added after the first time messages were read.
   * @param folderName folder to read from
   * @param callback
   * @group Functions
   */
  def readLatest(folderName: String, callback: ScriptCallback): Unit = MailUtils.readLatest(this, folderName, callback)

  /**
   * Read all messages from Inbox
   * @group Functions
   */
  def getEmails(): Array[MailMessage] = getEmails("Inbox", 0)

  /**
   * Get emails from Inbox
   * @param limit use this to limit how many messages. Eg. a limit of 10 will return the latest 10 messages. 0 returns all messages
   * @group Functions
   */
  def getEmails(limit: Int): Array[MailMessage] = getEmails("Inbox", limit)

  /**
   * Return all emails before a given date
   * @param date
   * @return
   */
  def getEmailsBefore(folder: String, date: java.util.Date) = MailUtils.getEmailsBefore(this, folder, date)

  /**
   * Get all emails after a given date
   * @param folder
   * @param date
   * @return
   */
  def getEmailsAfter(folder: String, date: Date) = MailUtils.getEmailsAfter(this, folder, date)

  /**
   * Get all emails from a specified folder
   * @param folderName
   * @group Functions
   */
  def getEmails(folderName: String): Array[MailMessage] = getEmails(folderName, 0)

  /**
   * Get emails from a specified folder with a specified limit
   *
   * @param folderName
   * @param limit
   * @group Functions
   */
  def getEmails(folderName: String, limit: Int): Array[MailMessage] = MailUtils.getEmails(this, folderName, limit)

  /**
   *
   * @param folderName
   * @param startUID
   * @group Functions
   */
  def getEmailsAfter(folderName: String, startUID: java.lang.Long): Array[MailMessage] = {
    MailUtils.getEmailsAfter(this, folderName, startUID)
  }

  /**
   * Returns true if this is a gmail account
   */
  def isGmail() = getUser.endsWith("gmail.com")

  //
  // Folder support
  //

  /**
   * Returns true if folder exists, false otherwise
   * @param folderName
   */
  def hasFolder(folderName : String): Boolean = MailUtils.hasFolder(this, folderName)

  /**
   * Return all of the folder names for this account
   */
  def getFolders(): Array[String] = MailUtils.getFolderNames(this)

  val Trash = if (isGmail) "Deleted Messages" else "Trash"

  private val gmailTopLevelFolders = Set("inbox", "deleted messages", "drafts", "sent", "sent messages")

  def getFolderName(name: String): String = {
    if (!isGmail)
      return name

    if (name.startsWith("[") || gmailTopLevelFolders.contains(name.toLowerCase))
      return name

    "[Gmail]/" + name
  }
}

object EmailAccount {

  def apply(imapHost: String, user: String, password: String, smtpHost: String): EmailAccount = {
    val bean = new EmailAccount
    bean.setImapHost(imapHost)
    bean.setUser(user)
    bean.setPassword(password)
    bean.setSmtpHost(smtpHost)

    bean
  }

  def toSmtpProperties(bean: EmailAccount):Properties = {
    val props = new Properties()
    props.put("mail.smtp.host", bean.getSmtpHost())
    props.put("mail.smtp.socketFactory.port", bean.getSmtpPort().toString)
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.port", bean.getSmtpPort().toString)
    props
  }
}
