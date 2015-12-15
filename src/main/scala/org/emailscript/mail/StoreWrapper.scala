package org.emailscript.mail

import java.util
import java.util.{Properties, Date}
import javax.mail.search.{ComparisonTerm, ReceivedDateTerm}
import javax.mail._

import com.sun.mail.imap.{IMAPFolder, IMAPStore, IMAPMessage}
import org.emailscript.api
import org.emailscript.api._
import org.emailscript.helpers._
import scala.collection.JavaConverters._

/**
  * Handles IMap stores
  */
class StoreWrapper(val account: EmailAccountBean, yaml: Yaml = Yaml(Configuration.DataDir)) {

  import StoreWrapper._

  val store = createStore()

  var idles: Map[String, ScanInfo] = Map()

  def scanFolder(folderName: String, callback: ProcessCallback, doFirstRead: Boolean = true): Unit = {
    checkStore()
    val info = ImapFolderScanner.scanFolder(this, folderName, callback, doFirstRead)
    idles = idles + (folderName -> info)
  }

  def checkStore(): Boolean = {
    if (!store.isConnected) {
      logger.info(s"store connecting to account: ${account.imapHost} user: ${account.user}")
      store.connect(account.imapHost, account.user, account.password)
      true
    } else {
      false
    }
  }

  def getFolder(folderName: String): IMAPFolder = {
    checkStore()
    val folder = store.getFolder(folderName).asInstanceOf[IMAPFolder]
    folder.open(MailUtils.defaultPermissions)
    folder
  }

  type Source = (IMAPFolder) => Array[Email]

  def foreach(folderName: String, source: Source, callback: Callback): Long = {

    var uid: Long = 0
    val folder = getFolder(folderName)

    try {
      source(folder).foreach { email: Email =>
        uid = email.getUid()
        callback(email)
      }
    } catch {
      case e: Throwable => logger.warn("", e)
    }

    folder.close(!MailUtils.dryRun)
    uid
  }

  def getEmails(messages: Array[Message], folder: IMAPFolder) = {
    fetch(messages, folder)
    messages.map { case m:IMAPMessage => Email(new MailMessageHelper(m, account))}
  }

  def getEmails(folder: IMAPFolder, limit:Int = 0): Array[Email] = {

    val start = if (limit <= 0 || limit >= folder.getMessageCount) 1 else folder.getMessageCount - limit + 1
    val count = folder.getMessageCount

    val messages = folder.getMessages(start, count)
    getEmails(messages, folder)
  }

  def getEmailsReversed(folder: IMAPFolder, limit: Int =0): Array[Email] = getEmails(folder, limit).reverse

  def getEmailsBeforeDate(folder: IMAPFolder, date: java.util.Date) = {
    val olderThan = new ReceivedDateTerm(ComparisonTerm.LT, date)

    val emails = folder.search(olderThan)

    getEmails(emails, folder)
  }

  def getEmailsAfterDate(folder: IMAPFolder, date: java.util.Date) = {
    val newerThan = new ReceivedDateTerm(ComparisonTerm.GT, date)

    val emails = folder.search(newerThan)

    getEmails(emails, folder)
  }

  def getEmailSafe(folder: IMAPFolder, id: Long): Option[Message] = {
    Option(folder.getMessageByUID(id))
  }

  def getEmailsByUID(folder: IMAPFolder, ids: util.ArrayList[Number]): Array[Email] = {
    val scalaIds = ids.asScala.toArray
    getEmailsByUID(folder, scalaIds)
  }

  def getEmailsByUID(folder: IMAPFolder, ids: Array[Number]): Array[Email] = {
    val messages = ids.flatMap{id => getEmailSafe(folder, id.longValue())}
    getEmails(messages, folder)
  }

  def foreachAfterUID(folderName: String, startUID: Long, callback: Callback): Long = {

    def get(folder: IMAPFolder) = getEmailsAfterUID(folder, startUID)
    foreach(folderName, get, callback)
  }

  def getEmailsAfterUID(folder: IMAPFolder, startUID: java.lang.Long): Array[Email]  = {
    val start: Long  =  if (startUID == null || startUID < 0) 0  else startUID
    val messages = folder.getMessagesByUID(start + 1, UIDFolder.LASTUID)

    fetch(messages, folder)

    // Get rid of final message (JavaMail insists on including the very last message when using LASTUID)
    val filtered = messages.filter{ m: Message => m.getFolder.asInstanceOf[UIDFolder].getUID(m) > start }
    filtered.map { case m:IMAPMessage => Email(new MailMessageHelper(m, account))}
  }

  def processLatest(dataName: String, folderName: String, callback: ProcessCallback): Unit = {

    val name = MailUtils.createDataName(account, dataName, folderName)
    val lastScan = yaml.getOrElse(name, () => new LastScan)
    logger.info(s" checking for emails in ${folderName}; last scan: $lastScan")
    lastScan.start = new Date()

    val lastId = foreachAfterUID(folderName, lastScan.lastId, callback.callback)

    Tags.save()
    Values.save()

    if (lastId > 0)
      lastScan.lastId = lastId

    lastScan.stop = new Date()
    yaml.set(name, lastScan)

  }

  def getFolders(): Array[api.Folder] = {
    checkStore()
    store.getDefaultFolder.list("*").map{ f => new api.Folder(f.asInstanceOf[IMAPFolder]) }
  }

  def hasFolder(name: String): Boolean = {
    val folderName = MailUtils.getFolderName(account, name)
    getFolder(name).exists()
  }
}

object StoreWrapper {
  val logger = LoggerFactory.getLogger(getClass)

  type Callback = (Email) => Unit
  val defaultFetchProfile = createDefaultFetchProfile()

  def fetch(messages: Array[Message], folder: IMAPFolder): Unit = {

    logger.debug(s"fetching ${messages.length} email(s) from ${folder.getName}")
    folder.fetch(messages,defaultFetchProfile)
    logger.debug(s"finishing fetch for ${folder.getName()}")
  }

  def createStore(): IMAPStore = {
    val session: Session = Session.getDefaultInstance(new Properties(), null)
    session.getStore("imaps").asInstanceOf[IMAPStore]//For now at least, only support ssl connections
  }

  private def createDefaultFetchProfile() = {
    val fp = new FetchProfile()
    fp.add(FetchProfile.Item.ENVELOPE)
    fp.add(FetchProfile.Item.FLAGS)
    fp.add(FetchProfile.Item.SIZE)
    fp.add(UIDFolder.FetchProfileItem.UID)
    fp.add(MailUtils.MoveHeader)
    fp
  }

}
