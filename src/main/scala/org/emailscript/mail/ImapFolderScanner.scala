package org.emailscript.mail

import java.time.Duration
import javax.mail.FolderClosedException

import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPFolder.ProtocolCommand
import com.sun.mail.imap.protocol.IMAPProtocol
import org.emailscript.api.{LastScan, EmailAccount, ProcessCallback}
import org.emailscript.helpers.{Yaml, Values, Tags, LoggerFactory}


/**
  * Utility to continuously scan for mails added to an imap folder
  *
  * Uses the Imap IDLE command to wait until emails arrive
  * But sometimes a server will silently timeout while we are in IDLE and we stop getting messages
  *
  * So we use a separate thread to time out the IDLE by sending a NOOP command to the email server.
  *
  * This does two things. It is a keepalive
  * for our server network connection and it also pops us out of our IDLE wait.
  *
  * We then can start a fresh IDLE session with the server
  *
  * See discussion here: http://stackoverflow.com/questions/4155412/javamail-keeping-imapfolder-idle-alive
 */
class ImapFolderScanner(store: StoreWrapper,
                        folderName: String,
                        doFirstRead: Boolean,
                        callback: ProcessCallback) extends Runnable {

  import ImapFolderScanner._

  val dataName = "LastScan"

  // Thread to periodically sent NOOP messages to the server. The mail server will then send a message to us which will break
  // us out of our IDLE

  class TimeOut(folder: IMAPFolder) {

    val thread = new Thread(folderName + "-Idle") {
      override def run() {
        try {

          logger.debug(s"Thread $getName is sleeping...")
          Thread.sleep(KEEP_ALIVE_FREQ)
          logger.debug(s"Thread $getName waking up...")

          // Perform a NOOP to have us exit from idle
          logger.info(s"Performing a NOOP on ${folderName} to trigger exit from IDLE")
          if (folder.isOpen)
            folder.doCommand(noopCommand)
        }
        catch {
          // InterruptedException means someone woke us from our sleep. Time for us to exit.
          case e: InterruptedException => logger.info(s"Thread $getName interrupted")

          case e: Throwable => logger.warn(s"Unexpected exception in Thread $getName", e)
        } finally {
        }

        logger.debug(s"Thread $getName finished")
      }
    }

    def interrupt() = {thread.interrupt()}
    def start() = thread.start()
  }


  override def run() {

    // Run the callback right away
    if (doFirstRead)
      store.processLatest(dataName, folderName, callback)

    // Now start our IDLE

    var waitMillis = 0L
    var timeOut: TimeOut = null
    var folder: IMAPFolder = null

    while (!Thread.interrupted()) {
      logger.debug("Starting IDLE")
      try {

        if (waitMillis > 0) {
          logger.info(s"Sleeping ${waitMillis/1000} seconds to give email server time to restart")
          Thread.sleep(waitMillis)
        }

        waitMillis = WaitMillis
        folder = store.getFolder(folderName)
        timeOut = new TimeOut(folder)
        timeOut.start

        waitMillis = 0L
        folder.idle(true)
        logger.debug("returning from idle")
        timeOut.interrupt()
        timeOut = null

        store.processLatest(dataName, folderName, callback)

        MailUtils.closeFolder(folder)
      }
      catch {
        case closed: FolderClosedException =>
          logger.warn(s" Folder ${folderName} is closed.", closed)
          waitMillis = WaitMillis
        case error: javax.mail.StoreClosedException =>
          logger.warn(s" The javamail Store for Folder $folderName is closed", error)
          waitMillis = WaitMillis
        case error: javax.mail.FolderClosedException =>
          logger.warn(s" Folder ${folderName} is closed", error)
          waitMillis = WaitMillis
        case error: IllegalStateException =>
          logger.warn(s"Folder ${folderName} illegal state", error)
          waitMillis = WaitMillis
        case e: Throwable =>
          logger.error(s"Error running scanning callback on folder: $folderName", e)
          throw new RuntimeException(e)
      } finally {
          MailUtils.closeFolder(folder)
          if (timeOut != null)
            timeOut.interrupt()
      }
    }

    logger.info("stopped listening")
  }
}

case class ScanInfo(scanner: ImapFolderScanner, interruptThread: Thread)

object ImapFolderScanner {

  val logger = LoggerFactory.getLogger(getClass)
  val KEEP_ALIVE_FREQ = Duration.ofMinutes(9).toMillis // rumor has it that gmail only allows 10 minute connections
  val WaitMillis = Duration.ofSeconds(5).toMillis

  val noopCommand = new ProtocolCommand {
    override def doCommand(protocol: IMAPProtocol): Object = {
      protocol.simpleCommand("NOOP", null)
      null
    }
  }

  def scanFolder(store: StoreWrapper, folderName: String,
                 callback: ProcessCallback, doFirstRead: Boolean): ScanInfo = {

    val scanner = new ImapFolderScanner(store, folderName, doFirstRead, callback)
    val listenerThread = new Thread(scanner, folderName + "-Scanner" )

    listenerThread.start()

    ScanInfo(scanner, listenerThread)
  }


}