package org.emailscript

import java.io.File

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.util.StatusPrinter
import com.google.common.io.Files
import org.emailscript.helpers.{Values, Tags, ScriptHelper}
import org.emailscript.mail.MailUtils
import org.slf4j.{LoggerFactory, MDC}

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

    val usage = "[-h | --help] [-debugLogging] [-dryrun] scriptname"
    try {

      //
      // Read arguments
      //

      if (args.contains("-h") || args.contains("--help")){
        println(s"Usage: $usage")
        return
      }

      if (args.contains("-debugLogging"))
        debugLogging()

      MailUtils.dryRun = if (args.contains("-dryrun")) true else false
      if (MailUtils.dryRun){
        logger.info("Running in debug mode")
      }

      //
      // Read in script
      //

      val scriptName = if (args.length == 0) "main.groovy" else args(args.length - 1)
      val script = new File(scriptName)
      if (!script.exists) {
        throw new Exception(s"{script.getName} not found! Usage: ${usage}")
      }

      // Configure our logger to output to <scriptName>.log (see logback.xml config file)
      MDC.put("script", Files.getNameWithoutExtension(scriptName))



      val result = ScriptHelper.runScript(script)

      logger.info(s"finished processing; result: ${result}")
    }
    catch {
      case e: Throwable => logger.error("Fatal Error", e)
    }

    Tags.save()
    Values.save()

    MailUtils.close()

  }

}
