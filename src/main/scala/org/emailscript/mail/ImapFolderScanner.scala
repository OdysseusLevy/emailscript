package org.emailscript.mail

import java.time.Duration
import javax.mail.FolderClosedException

import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPFolder.ProtocolCommand
import com.sun.mail.imap.protocol.IMAPProtocol
import org.emailscript.api.{EmailAccount, ProcessCallback}
import org.emailscript.helpers.LoggerFactory


/**
  * Utility to continuously scan for mails added to an imap folder
  *
  * Uses the Imap IDLE command to wait until emails arrive
  * But sometimes a server will silently timeout while we are in IDLE and we stop getting messages
  *
  * So we use a separate thread to periodically send NOOPs to the email server. This does two things. It is a keepalive
  * for our server network connection and it also pops us out of our IDLE wait.
  *
  * We then can start a fresh IDLE session with the server
  *
  * Code adapted from an answer found here: http://stackoverflow.com/questions/4155412/javamail-keeping-imapfolder-idle-alive
 */
class ImapFolderScanner(account: EmailAccount, folder: IMAPFolder, doFirstRead: Boolean, callback: ProcessCallback) extends Runnable {

  import ImapFolderScanner._

  val dataName = "LastScan"

  // Thread to periodically sent NOOP messages to the server. The mail server will then send a message to us which will break
  // us out of our IDLE

  val keepAlive = new Thread(folder.getName + "-Idle" ){

    override def run() {
      while (!isInterrupted) {
        try {

          logger.debug(s"Thread $getName is sleeping...")
          Thread.sleep(KEEP_ALIVE_FREQ)

          // Perform a NOOP to have us exit from idle
          logger.info(s"Performing a NOOP on ${folder.getName} to trigger exit from IDLE")
          MailUtils.ensureOpen(account, folder)
          folder.doCommand(noopCommand)
        }
        catch {
          // InterruptedException means someone woke us from our sleep. Time for us to exit.
          // The interrupted flag is not set on this thread when this happens so we need to set it ourselves
          case e: InterruptedException =>  interrupt(); logger.info(s"Thread $getName INTERRUPTED")

          case e: Throwable => logger.warn("Unexpected exception while keeping alive the IDLE connection", e)
        }
      }

      logger.info(s"Thread $getName finished")
    }
  }

  override def run() {

    var currentFolder = folder

    // Run the callback right away
    if (doFirstRead)
      MailUtils.doCallback(account, dataName, currentFolder, callback)


    // Start a separate thread to periodically interrupt us

    keepAlive.start()

    // Now start our IDLE

    var waitMillis = 0L
    while (!Thread.interrupted()) {
      logger.debug("Starting IDLE")
      try {

        if (waitMillis > 0) {
          logger.info(s"Sleeping ${waitMillis/1000} seconds to give email server time to restart")
          Thread.sleep(waitMillis)
        }

        MailUtils.ensureOpen(account, folder)
        waitMillis = 0L

        folder.idle(true)
        logger.debug("returning from idle")
        MailUtils.doCallback(account, dataName, folder, callback)

        // Folders keep a cache of all opened messages. This is a memory leak. Only folder.close() will flush this.

        // REVIEW: closing/reopening might be expensive network wise, consider closing less often or opening using the
        // ResyncData info (to minimize network roundtrips)

        logger.debug(s"closing folder ${folder.getName}")
        folder.close(true)
      }
      catch {
        case closed: FolderClosedException =>
          logger.warn(s" Folder ${folder.getName} is closed. isOpen: ${folder.isOpen}", closed)
          waitMillis = WaitMillis
        case error: javax.mail.StoreClosedException =>
          logger.warn(s" The javamail Store for Folder ${folder.getName} is closed", error)
          waitMillis = WaitMillis
        case error: javax.mail.FolderClosedException =>
          logger.warn(s" Folder ${folder.getName} is closed", error)
          waitMillis = WaitMillis
        case error: IllegalStateException =>
          logger.warn(s"Folder ${folder.getName} illegal state", error)
          waitMillis = WaitMillis
        case e: Throwable =>
          logger.error(s"Error running scanning callback on folder: ${folder.getName}", e)
          keepAlive.interrupt() //we want both threads to stop now
          throw new RuntimeException(e)
      }
    }

    logger.info("stopped listening")
    stop()
  }

  def stop(): Unit = {
    // Shutdown keep alive thread
    if (keepAlive.isAlive) {
      logger.info("interrupting keepAlive")
      keepAlive.interrupt()
    }
  }
}

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

  var threads = Map[IMAPFolder, Thread]()

  def isDone = threads.size == 0

  def scanFolder(account: EmailAccount, folder: IMAPFolder, callback: ProcessCallback, doFirstRead: Boolean): Unit = {

    val scanner = new ImapFolderScanner(account, folder, doFirstRead, callback)
    val listenerThread = new Thread(scanner, folder.getName + "-Scanner" )

    threads = threads + (folder -> listenerThread)
    listenerThread.start()
  }
}