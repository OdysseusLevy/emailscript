package org.emailscript

import java.io.File

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import com.google.common.io.Files
import org.slf4j.{LoggerFactory, MDC}

case class Account(hostName: String, userName: String, password: String)

object Emailscript {

  val logger = LoggerFactory.getLogger(getClass)

  /**
   * Use this when something is not working with the logger
   */
  def debugLogging() {
    val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    StatusPrinter.print(lc)
  }

  def main(args: Array[String]) {

    try {

      val scriptName = if (args.length == 0) "main.groovy" else args(0)
      val script = new File(scriptName)
      if (!script.exists)
        throw new Exception(script.getName + "not found!")

      // Configure our logger to output to <scriptName>.log (see logback.xml config file)
      MDC.put("script", Files.getNameWithoutExtension(scriptName))

      MailUtils.dryRun = if (args.contains("-dryrun")) true else false
      if (MailUtils.dryRun){
        logger.info("Running in debug mode")
      }

      val result = ScriptHelper.runScript(script)

      logger.info(s"finished processing; result: ${result}")
    }
    catch {
      case e: Throwable => logger.error("Fatal Error", e)
    }

    Tags.save()
    Values.save()

    logger.info(s"closing connections")
    MailUtils.close()

  }

}
