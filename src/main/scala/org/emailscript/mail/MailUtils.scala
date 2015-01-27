package org.emailscript.mail

import java.util.{Date, Properties}
import javax.mail.Flags.Flag
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.search.{ComparisonTerm, ReceivedDateTerm}

import com.sun.mail.imap.{IMAPFolder, IMAPMessage}
import com.sun.mail.pop3.POP3Folder
import org.emailscript.api._
import org.emailscript.helpers.{Yaml, Values, Tags}
import org.slf4j.LoggerFactory

import scala.collection.immutable.HashMap

/**
 * Basic mail handling utilities using javax.mail
 */
object MailUtils {

  val yaml = Yaml()
  val MoveHeader = "Mailscript-Move"
  val logger = LoggerFactory.getLogger(getClass)
  var folders = new HashMap[String, Folder]()
  var accounts = new HashMap[EmailAccount,Store]()

  var dryRun: Boolean = false
  lazy val defaultPermissions = if (dryRun) Folder.READ_ONLY else Folder.READ_WRITE

  def ensureOpen(folder: Folder) = {
    if (!folder.isOpen){ //Just in case we get timed out
      logger.warn(s"reopening folder: ${folder.getName}")
      folder.open(MailUtils.defaultPermissions)
    }
  }

  def getStore(account: EmailAccount) = accounts.getOrElse(account, connect(account))

  def sendMessage(account: EmailAccount, messageBean: EmailBean) = {

    logger.info(s"sending email to ${messageBean.getTo}")
    val session = Session.getInstance(EmailAccount.toSmtpProperties(account))
    val message = messageBean.toMessage(session)
    Transport.send(message, account.getUser, account.getPassword)
  }

  private def openFolder(folder: Folder, permissions: Int = defaultPermissions) = {
    try {
      folder.open(defaultPermissions)
      folders += (folder.getFullName -> folder)
    } catch {
      case e: Throwable => logger.error(s"Error trying to open ${folder.getFullName}", e); throw e
    }
  }

  private def openFolder(store: Store, folderName: String): Folder = {
    val folder = store.getFolder(folderName)
    openFolder(folder)
    folder
  }

  def getFolder(account: EmailAccount, store: Store, name: String): Folder = {

    val folderName = EmailAccount.getFolderName(account, name)
    if(folders.contains(folderName))
      folders(folderName)
    else
      store.getFolder(folderName)
  }

  def openFolder(account: EmailAccount, folderName: String): Folder = {
    val store = getStore(account)
    openFolder(store, EmailAccount.getFolderName(account, folderName))
  }

  def readLatest(account: EmailAccount, folderName: String, callback: ScriptCallback): Unit = {
    val folder = openFolder(account, folderName)
    val dataName = folderName + "LastRead"
    doCallback(account, dataName, folder.asInstanceOf[IMAPFolder], callback)
  }

  def scanFolder(account: EmailAccount, folderName: String, callback: ScriptCallback, doFirstRead: Boolean = true): Unit = {
    val mailFolder = openFolder(account, folderName)
    ImapFolderScanner.scanFolder(account, mailFolder.asInstanceOf[IMAPFolder], callback, doFirstRead)
  }

  def fetch(messages: Array[Message], folder: Folder) = {
    logger.info(s"fetching ${messages.length} email(s) from ${folder.getName()}")
    val fp = new FetchProfile()
    fp.add(FetchProfile.Item.ENVELOPE)
    fp.add(FetchProfile.Item.FLAGS)
    fp.add(FetchProfile.Item.SIZE)
    fp.add(MoveHeader)

    folder.fetch(messages,fp)
    logger.info(s"finishing fetch for ${folder.getName()}")
  }

  def convertMessages(account: EmailAccount, messages: Array[Message], folder: Folder) = {
    fetch(messages, folder)
    messages.map { case m:IMAPMessage => MailMessage(new MailMessageHelper(account, m))}
  }

  def getEmailsBefore(account: EmailAccount, folderName: String, date: java.util.Date) = {
    val olderThan = new ReceivedDateTerm(ComparisonTerm.LT, date)

    val folder = openFolder(account, folderName)
    val emails = folder.search(olderThan)

    convertMessages(account, emails, folder)
  }

  def getEmailsAfter(account: EmailAccount, folderName: String, date: java.util.Date) = {
    val newerThan = new ReceivedDateTerm(ComparisonTerm.GT, date)

    val folder = openFolder(account, folderName)
    val emails = folder.search(newerThan)

    convertMessages(account, emails, folder)
  }

  def getEmails(account: EmailAccount, folderName: String, limit:Int = 0): Array[MailMessage] = {

    val mailFolder = openFolder(account, folderName)
    val start = if (limit <= 0 || limit >= mailFolder.getMessageCount) 1 else mailFolder.getMessageCount - limit + 1
    val count = mailFolder.getMessageCount

    val messages = mailFolder.getMessages(start, count)
    convertMessages(account, messages, mailFolder)
  }

  def getEmailsAfter(account: EmailAccount, folderName: String, startUID: java.lang.Long): Array[MailMessage] = {
    val folder = openFolder(account, folderName).asInstanceOf[IMAPFolder]
    getEmailsAfter(account, folder, startUID)
  }

  def getEmailsAfter(account: EmailAccount, folder: IMAPFolder, startUID: java.lang.Long): Array[MailMessage]  = {
    val start: Long  =  if (startUID == null || startUID < 0) 0  else startUID
    val messages = folder.getMessagesByUID(start + 1, UIDFolder.LASTUID)

    fetch(messages, folder)

    // Get rid of final message (JavaMail insists on including the very last message when using LASTUID)
    val filtered = messages.filter{ m: Message => m.getFolder.asInstanceOf[UIDFolder].getUID(m) > start }
    filtered.map { case m:IMAPMessage => MailMessage(new MailMessageHelper(account, m))}
  }

  def moveTo(toFolderName: String, m: MailMessageHelper) {

    if (dryRun) {
      logger.info(s"DRY RUN -- moving message from: ${m.from} subject: ${m.subject} to folder: $toFolderName")
      return
    }

    val fromFolder: Folder = m.message.getFolder
    val store = fromFolder.getStore
    val toFolder = getFolder(m.account, store, toFolderName)

    if (!toFolder.isOpen)
      openFolder(toFolder, Folder.READ_WRITE)

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
    store.connect(account.getImapHost, account.getUser, account.getPassword)
    accounts = accounts + (account -> store)
    store
  }

  def expunge(folder: Folder):Unit = {
    if (!dryRun)
      folder.expunge()
  }

  def close(): Unit = {

    if (ImapFolderScanner.isDone){
      logger.info(s"closing connections")

      val expunge = !dryRun

      folders.values.foreach( (folder: Folder) => if (folder.isOpen) folder.close(expunge))
      accounts.values.foreach(_.close())
    } else {
      logger.info("Exiting main program, but scanning threads are still running")
    }

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

  def getUID(folder: Folder, m: Message): Long = {
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

  def getFolders(account: EmailAccount): Array[Folder] = {
      getStore(account).getDefaultFolder.list("*").filter{
        f => (f.getType & javax.mail.Folder.HOLDS_MESSAGES) != 0
      }
  }

  def getFolderNames(account: EmailAccount): Array[String] = {
    getFolders(account).map(_.getFullName)
  }

  def hasFolder(account: EmailAccount, name: String): Boolean = {
    val folderName = EmailAccount.getFolderName(account, name)
    getStore(account).getFolder(folderName).exists()
  }

  def doCallback(account: EmailAccount, dataName: String, folder: IMAPFolder, callback: ScriptCallback): Unit = {
    val lastScan = yaml.getOrElse(dataName, () => new LastScan)
    logger.info(s" checking for emails in ${folder.getName}; last scan: $lastScan")
    lastScan.start = new Date()

    val emails = getEmailsAfter(account, folder, lastScan.lastId)
    if (emails.length == 0) {
      logger.info(s"No emails found in ${folder.getName}")
    }
    else {
      logger.info(s"${emails.size} new email(s) found in ${folder.getName}")

      try {
        callback.callback(emails)
      }
      catch {
        case e: Throwable => throw new Exception("Error running callback", e)
      }

      Tags.save()
      Values.save()
      MailUtils.expunge(folder)

      lastScan.stop = new Date()
      lastScan.lastId = emails.last.getUid
      yaml.set(dataName, lastScan)
    }
  }

  def dumpStructure(part: Part, prefix: String = ""): Unit ={

    logger.info(s"$prefix${part.getContentType()}")

    if (part.isMimeType("multipart/*")){
      val multi = part.getContent.asInstanceOf[Multipart]
      for(i <- 0 until multi.getCount ) {
        val mp = multi.getBodyPart(i)
          dumpStructure(mp, prefix + "\t")
      }
    }
  }

  def getMultiPartText(multi: Multipart): Option[String] = {

    for(i <- 0 until multi.getCount) {
      val part = multi.getBodyPart(i)

      if (part.isMimeType("text/*"))
        return Option(part.getContent.toString)

      val result = getBodyText(part)
      if (result.isDefined)
        return result
    }

    None
  }

    /**
     * Simplified algorithm for extracting a message's text. When presented with alternatives it will always
     * choose the 'preferred' format (which turns out to be html)
     *
     * Note that with complicated formats (such as lots of attachments with text interspersed between them) it will
     * simply return the first text block it finds
     *
     * @return null if no text is found
     */
  def getBodyText(part: Part): Option[String] = {

    part.getContent match {
      case text: String => Option(text)
      case multi: Multipart =>
        if (part.isMimeType("multipart/alternative"))
          getBodyText(multi.getBodyPart(multi.getCount -1)) // the last one is the 'preferred' version
         else
          getMultiPartText(multi)
      case _ => None
    }
  }
}
