package org.emailscript.api

import java.util.{Date, Properties}

import com.sun.mail.imap.IMAPFolder
import org.emailscript.helpers.{Yaml, Importer}
import org.emailscript.mail.{StoreWrapper, MailUtils}

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
class EmailAccount(bean: EmailAccountBean) {

  import EmailAccount._

  val store = new StoreWrapper(bean)

  /**
   * Create an email object that can be sent
   * @group Functions
   */
  def newMail() = EmailBean(bean.user)

  /**
   * Send an email
   * @param message email to send
   * @group Functions
   */
  def send(message: EmailBean) = MailUtils.sendMessage(bean, message)

  /**
   * Continuously scan the folder waiting for new messages to appear.
   * The first time this is run the entire contents of the folder will be scanned. After that the callback will be
   * triggered whenever new messages are added to the folder
   *
   * @param folderName folder to Scan
   * @param scanner callback
   * @group Functions
   */
  def scanFolder(folderName: String, scanner: ProcessCallback): Unit = store.scanFolder(folderName, scanner)

  /**
   * Scan folder (same as above), but with option to not do the first, initial read
   *
   * @param folderName folder to scan
   * @param doFirstRead if true will first read unread messages
   * @param scanner user supplied callback to handle the emails
   */
  def scanFolder(folderName: String, doFirstRead: Boolean,  scanner: ProcessCallback): Unit =
    store.scanFolder(folderName, scanner, doFirstRead)

  /**
   * Read in all new messages. The first time this is run it will read in all messages. Subsequent calls will only return
   * messages that have been added after the first time messages were read.
   * @param folderName folder to read from
   * @param callback user supplied callback to handle the emails
   * @group Functions
   */
  def readLatest(folderName: String, callback: ProcessCallback): Unit = {
    store.processLatest("LastRead", folderName, callback)
  }

  /**
   * Runs a script against all inbox emails
   * @group Functions
   */
  def foreach(script: ProcessCallback): Unit = {

    def get(folder: IMAPFolder) = store.getEmails(folder)
    store.foreach(Inbox ,get, script.callback)
  }


  /**
    * Runs a script against every email in a given folder
    * order is oldest to newest
    *
    * @param folderName folder to read in
    *  @group Functions
    */
  def foreach(folderName: String, script: ProcessCallback): Unit = {
    def get(folder: IMAPFolder) = store.getEmails(folder)
    store.foreach(folderName ,get, script.callback)
  }

  /**
    * Runs a script against every email in a given folder
    * order is newest to oldest
    *
    * @param folderName
    * @param script
    */
  def foreachReversed(folderName: String, script: ProcessCallback): Unit = {
    def get(folder: IMAPFolder) = store.getEmailsReversed(folder)
    store.foreach(folderName, get, script.callback)
  }
  /**
   * Runs a script against a list of emails in Inbox with the given uid
   *
   * @param ids array of specific uid's of the desired emails
   * @param script user callback
   * @group Functions
   */
  def foreach(ids: java.util.ArrayList[Number], script:ProcessCallback): Unit = {
    def get(folder: IMAPFolder) = store.getEmailsByUID(folder, ids)
    store.foreach(Inbox, get, script.callback)
  }

  /**
   * Run a script against emails from a given list of ids and folder
   *
   * @param folderName folder to read from
   * @param ids email uids
   * @param script script to run against each
   * @group Functions
   */
  def foreach(folderName: String, ids: java.util.ArrayList[Number], script: ProcessCallback): Unit = {
    def get(folder: IMAPFolder) = store.getEmailsByUID(folder, ids)
    store.foreach(folderName, get, script.callback)
  }

  /**
   * Runs a script against a given folder and limit of how many
   * @param folderName folder to read (eg. Inbox, Junk, etc.)
   * @param limit returns newest emails up to this limit
   * @param script
   *  @group Functions
   */
  def foreach(folderName: String, limit: Int, script: ProcessCallback): Unit = {
    def get(folder: IMAPFolder) = store.getEmails(folder, limit)
    store.foreach(folderName ,get, script.callback)
  }

  /**
   * Runs a script against the Inbox with a given limit of how many email to read
   *
   * @param limit returns newest emails up to this limit
   * @param script closure to run against each
   *  @group Functions
   */
  def foreach(limit: Int, script: ProcessCallback): Unit = {
    def get(folder: IMAPFolder) = store.getEmails(folder, limit)
    store.foreach(Inbox ,get, script.callback)
  }

  /**
    * Runs a script against all emails before a given date
    *
    * @param folderName folder to read
    * @param date date before which we are interested
    * @param script closure to run against each
    */
  def foreachBefore(folderName: String, date: Date, script: ProcessCallback): Unit = {
    def get(folder: IMAPFolder) = store.getEmailsBeforeDate(folder, date)
    store.foreach(folderName ,get, script.callback)
  }

  /**
    * Runs a script against all emails after a given date
    *
    * @param folderName folder to read
    * @param date date after which we are interested
    * @param script closure to run against each
    */
  def foreachAfter(folderName: String, date: Date, script: ProcessCallback): Unit = {
    def get(folder: IMAPFolder) = store.getEmailsAfterDate(folder, date)
    store.foreach(folderName ,get, script.callback)
  }

  //
  // Folder support
  //

  /**
   * Returns true if folder exists, false otherwise
   * @param folderName
   * @group Functions
   */
  def hasFolder(folderName : String): Boolean = store.hasFolder(folderName)

  /**
   * Return all of the folder names for this account
   * @group Functions
   */
  def getFolders(): Array[Folder] = store.getFolders()

}

object EmailAccount {

  val Inbox = "Inbox"

  def apply(bean: EmailAccountBean) = {
    new EmailAccount(bean)
  }

  def createBean(imapHost: String, user: String, password: String, smtpHost: String): EmailAccountBean = {
    val bean = new EmailAccountBean
    bean.setImapHost(imapHost)
    bean.setUser(user)
    bean.setPassword(password)
    bean.setSmtpHost(smtpHost)

    bean
  }


}
