package org.emailscript.mail

import java.time.Duration
import javax.mail.{FolderClosedException, MessagingException}

import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPFolder.ProtocolCommand
import com.sun.mail.imap.protocol.IMAPProtocol
import org.emailscript.api.{EmailAccount, ScriptCallback}
import org.emailscript.helpers.LoggerFactory


/**
 * Utility to continuously scan for mails added to an imap folder
 *
 * Uses the Imap IDLE command to wait until emails arrive
 * Uses another thread to periodically refresh that IDLE (otherwise it will silently timeout)
 *
 * Code adapted from an answer found here: http://stackoverflow.com/questions/4155412/javamail-keeping-imapfolder-idle-alive
 */
class ImapFolderScanner(account: EmailAccount, folder: IMAPFolder, doFirstRead: Boolean, callback: ScriptCallback) extends Runnable {

  import ImapFolderScanner._

  val logger = LoggerFactory.getLogger(getClass)
  val NOOP = "NOOP"

  val dataName = folder.getName + "LastScan"

  // Thread to periodically sent keep alive (noop) messages to the server

  val keepAlive = new Thread(folder.getName + "-Idle" ){

    val noopCommand = new ProtocolCommand {
      override def doCommand(protocol: IMAPProtocol): Object = {
        protocol.simpleCommand(NOOP, null)
        null
      }
    }

    override def run() {
      while (!isInterrupted) {
        try {

          logger.debug(s"Thread $getName is sleeping...")
          Thread.sleep(KEEP_ALIVE_FREQ)

          // Perform a NOOP just to keep alive the connection
          logger.info("Performing a NOOP to keep the connection alive")
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

    // Run the callback right away
    if (doFirstRead)
      MailUtils.doCallback(account, dataName, folder, callback)


    // Start a separate thread to send keep alive messages to the server

    keepAlive.start()

    // Now go to sleep until the mail server wakes us up

    while (!Thread.interrupted()) {
      logger.debug("Starting IDLE")
      try {
        MailUtils.ensureOpen(account, folder)

        folder.idle(true)
        logger.debug("returning from idle")
        MailUtils.doCallback(account, dataName,folder, callback)

      }
      catch {
        case closed: FolderClosedException =>
          logger.warn(s" Folder ${folder.getName} is closed. isOpen: ${folder.isOpen}", closed)
        case error: javax.mail.StoreClosedException =>
          logger.warn(s" The javamail Store for Folder ${folder.getName} is closed", error)
        case error: javax.mail.FolderClosedException =>
          logger.warn(s" Folder ${folder.getName} is closed", error)
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

  var threads = Map[IMAPFolder, Thread]()

  def isDone = threads.size == 0

  def scanFolder(account: EmailAccount, folder: IMAPFolder, callback: ScriptCallback, doFirstRead: Boolean): Unit = {

    val scanner = new ImapFolderScanner(account, folder, doFirstRead, callback)
    val listenerThread = new Thread(scanner, folder.getName + "-Scanner" )

    threads = threads + (folder -> listenerThread)
    listenerThread.start()
  }
}