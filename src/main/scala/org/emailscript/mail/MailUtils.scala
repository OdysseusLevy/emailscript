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

import scala.collection.JavaConverters._
import scala.collection.immutable.HashMap

/**
 * Basic mail handling utilities using javax.mail
  * TODO Explicitly switch to supporting just IMAP folders
 */
object MailUtils {

  val yaml = Yaml(Configuration.DataDir)
  val MoveHeader = "Mailscript-Move"
  val logger = LoggerFactory.getLogger(getClass)
  val defaultFetchProfile = createDefaultFetchProfile()

  var folders = new HashMap[String, JavaMailFolder]()
  var accounts = new HashMap[EmailAccount,Store]()

  var dryRun: Boolean = false
  lazy val defaultPermissions = if (dryRun) JavaMailFolder.READ_ONLY else JavaMailFolder.READ_WRITE


  /**
   * Try to restore closed connections
   */
  def ensureOpen(account: EmailAccount, folder: JavaMailFolder) = {

    if (!folder.getStore.isConnected) {
      logger.info(s"reopening store connected to: ${folder.getName}")
      folder.getStore.connect(account.imapHost, account.user, account.password)
    }

    if (!folder.isOpen){
      logger.info(s"reopening folder: ${folder.getName}")
      folder.open(MailUtils.defaultPermissions)
    }
  }

  private def createDefaultFetchProfile() = {
    val fp = new FetchProfile()
    fp.add(FetchProfile.Item.ENVELOPE)
    fp.add(FetchProfile.Item.FLAGS)
    fp.add(FetchProfile.Item.SIZE)
    fp.add(UIDFolder.FetchProfileItem.UID)
    fp.add(MoveHeader)
    fp
  }

  def getStore(account: EmailAccount) = accounts.getOrElse(account, connect(account))

  def sendMessage(account: EmailAccount, messageBean: EmailBean) = {

    logger.info(s"sending email to ${messageBean.getTo}")
    val session = Session.getInstance(EmailAccount.toSmtpProperties(account))
    val message = messageBean.toMessage(session)
    Transport.send(message, account.user, account.password)
  }

  private def openFolder(folder: JavaMailFolder, permissions: Int = defaultPermissions) = {
    try {
      folder.open(defaultPermissions)
      folders += (folder.getFullName -> folder)
    } catch {
      case e: Throwable => logger.error(s"Error trying to open ${folder.getFullName}", e); throw e
    }
  }

  private def openFolder(store: Store, folderName: String): JavaMailFolder = {
    val folder = store.getFolder(folderName)
    openFolder(folder)
    folder
  }

  def getFolder(account: EmailAccount, store: Store, name: String): JavaMailFolder = {

    val folderName = EmailAccount.getFolderName(account, name)
    if(folders.contains(folderName))
      folders(folderName)
    else
      store.getFolder(folderName)
  }

  def openFolder(account: EmailAccount, folderName: String): JavaMailFolder = {
    val store = getStore(account)
    openFolder(store, EmailAccount.getFolderName(account, folderName))
  }

  def readLatest(account: EmailAccount, folderName: String, callback: ProcessCallback): Unit = {
    val folder = openFolder(account, folderName)
    doCallback(account, "LastRead", folder.asInstanceOf[IMAPFolder], callback)
  }

  def scanFolder(account: EmailAccount, folderName: String, callback: ProcessCallback, doFirstRead: Boolean = true): Unit = {
    val mailFolder = openFolder(account, folderName)
    ImapFolderScanner.scanFolder(account, mailFolder.asInstanceOf[IMAPFolder], callback, doFirstRead)
  }

  def fetch(messages: Array[Message], folder: JavaMailFolder): Unit = {

    logger.debug(s"fetching ${messages.length} email(s) from ${folder.getName}")
    folder.fetch(messages,defaultFetchProfile)
    logger.debug(s"finishing fetch for ${folder.getName()}")
  }

  def getEmails(account: EmailAccount, messages: Array[Message], folder: JavaMailFolder) = {
    fetch(messages, folder)
    messages.map { case m:IMAPMessage => Email(new MailMessageHelper(account, m))}
  }

  def getEmailsBefore(account: EmailAccount, folderName: String, date: java.util.Date) = {
    val olderThan = new ReceivedDateTerm(ComparisonTerm.LT, date)

    val folder = openFolder(account, folderName)
    val emails = folder.search(olderThan)

    getEmails(account, emails, folder)
  }

  def getEmailsAfter(account: EmailAccount, folderName: String, date: java.util.Date) = {
    val newerThan = new ReceivedDateTerm(ComparisonTerm.GT, date)

    val folder = openFolder(account, folderName)
    val emails = folder.search(newerThan)

    getEmails(account, emails, folder)
  }

  def getEmails(account: EmailAccount, folderName: String, limit:Int = 0): Array[Email] = {

    val mailFolder = openFolder(account, folderName)
    val start = if (limit <= 0 || limit >= mailFolder.getMessageCount) 1 else mailFolder.getMessageCount - limit + 1
    val count = mailFolder.getMessageCount

    val messages = mailFolder.getMessages(start, count)
    getEmails(account, messages, mailFolder)
  }

  def getEmailSafe(folder: IMAPFolder, id: Long): Option[Message] = {
    Option(folder.getMessageByUID(id))
  }

  def getEmails(account: EmailAccount, folderName: String, ids: util.ArrayList[Number]): Array[Email] = {
    val folder = openFolder(account, folderName).asInstanceOf[IMAPFolder] //TODO time to fess up and change all Folders to IMAPFolder
    val messages = ids.asScala.toArray.flatMap{id => getEmailSafe(folder, id.longValue())}
    getEmails(account, messages, folder)
  }

  def getEmailsAfter(account: EmailAccount, folderName: String, startUID: java.lang.Long): Array[Email] = {
    val folder = openFolder(account, folderName).asInstanceOf[IMAPFolder]
    getEmailsAfter(account, folder, startUID)
  }

  def getEmailsAfter(account: EmailAccount, folder: IMAPFolder, startUID: java.lang.Long): Array[Email]  = {
    val start: Long  =  if (startUID == null || startUID < 0) 0  else startUID
    val messages = folder.getMessagesByUID(start + 1, UIDFolder.LASTUID)

    fetch(messages, folder)

    // Get rid of final message (JavaMail insists on including the very last message when using LASTUID)
    val filtered = messages.filter{ m: Message => m.getFolder.asInstanceOf[UIDFolder].getUID(m) > start }
    filtered.map { case m:IMAPMessage => Email(new MailMessageHelper(account, m))}
  }

  def moveTo(toFolderName: String, m: MailMessageHelper) {

    if (dryRun) {
      logger.info(s"DRY RUN -- moving message from: ${m.from} subject: ${m.subject} to folder: $toFolderName")
      return
    }

    val fromFolder: JavaMailFolder = m.message.getFolder
    val store = fromFolder.getStore
    val toFolder = getFolder(m.account, store, toFolderName)

    if (!toFolder.isOpen)
      openFolder(toFolder, JavaMailFolder.READ_WRITE)

    val newMessage = new MimeMessage(m.message.asInstanceOf[MimeMessage])

    newMessage.removeHeader(MoveHeader)
    newMessage.addHeader(MoveHeader, toFolderName)

    val messageArray: Array[Message] = Array(newMessage)
    logger.info(s"moving mail from: ${m.from} subject: ${m.subject} to folder: $toFolderName")
    toFolder.appendMessages(messageArray)
    m.message.setFlag(Flag.DELETED, true)
  }

  protected def connect(account: EmailAccount): Store = {
    val session: Session = Session.getDefaultInstance(new Properties(), null)
    val store = session.getStore("imaps") //For now at least, only support ssl connections
    store.connect(account.imapHost, account.user, account.password)
    accounts = accounts + (account -> store)
    store
  }

  def expunge(folder: JavaMailFolder):Unit = {
    if (!dryRun)
      folder.expunge()
  }

  def close(): Unit = {

    if (ImapFolderScanner.isDone){
      logger.info(s"closing connections")

      val expunge = !dryRun

      folders.values.foreach( (folder: JavaMailFolder) => if (folder.isOpen) folder.close(expunge))
      accounts.values.foreach(_.close())
    } else {
      logger.info("Exiting main program, but scanning threads are still running")
    }

  }

  def saveToFile(message: IMAPMessage, file : File) = {
    message.writeTo(new FileOutputStream(file))
  }

  def delete(permanent: Boolean, m: MailMessageHelper): Unit = {

    if (dryRun) {
      logger.info(s"DRY RUN -- deleting message from: ${m.from} subject: ${m.subject}")
      return
    }

    if (permanent)
      m.message.setFlag(Flag.DELETED, true)
    else
      moveTo(EmailAccount.trashFolder(m.account), m)
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

  def getImapFolders(account: EmailAccount): Array[IMAPFolder] = {
      getStore(account).getDefaultFolder.list("*").map{ f => f.asInstanceOf[IMAPFolder] }
  }

  def getFolders(account: EmailAccount): Array[Folder] = {
    getImapFolders(account).map{ Folder(_)}
  }

  def hasFolder(account: EmailAccount, name: String): Boolean = {
    val folderName = EmailAccount.getFolderName(account, name)
    getStore(account).getFolder(folderName).exists()
  }

  def createDataName(account: EmailAccount, dataName: String, folder: IMAPFolder) = {
    s"${account.user}-${folder.getName}$dataName"
  }

  def doCallback(account: EmailAccount, dataName: String, folder: IMAPFolder, callback: ProcessCallback): Unit = {

    val name = createDataName(account, dataName, folder)
    val lastScan = yaml.getOrElse(name, () => new LastScan)
    logger.info(s" checking for emails in ${folder.getName}; last scan: $lastScan")
    lastScan.start = new Date()

    val emails = getEmailsAfter(account, folder, lastScan.lastId)
    if (emails.length == 0) {
      logger.info(s"No emails found in ${folder.getName}")
    }
    else {
      logger.info(s"${emails.length} new email(s) found in ${folder.getName}")

      try
        emails.foreach {
          callback.callback(_)
        }

      catch {
        case e: Throwable => throw new Exception("Error running callback", e)
      }

      Tags.save()
      Values.save()
      MailUtils.expunge(folder)

      lastScan.stop = new Date()
      lastScan.lastId = emails.last.getUid()
      yaml.set(name, lastScan)
    }
  }

}
