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
  val usage = "[-h | --help] [-debugLogging] [-dryrun] scriptname"

  /**
   * Use this when something is not working with the logger
   */
  def debugLogging() {
    val lc = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    StatusPrinter.print(lc)
  }

  def getScriptName(args: Array[String]): String = {
    if (args.length == 0)
      "main.groovy"
    else
      args(args.length - 1)
  }

  /**
   * Handle our command line arguments
   */
  def handleArgs(args: Array[String]): Option[File] = {

    if (args.contains("-h") || args.contains("--help")){
      return None
    }

    //
    // Read in script
    //

    val scriptName = getScriptName(args)
    val script = new File(scriptName)
    if (!script.exists) {
      println(s"${script.getName} not found!")
      return None
    }

    //
    // Configure our logger to output to <scriptName>.log (see logback.xml config file)
    //

    MDC.put("script", Files.getNameWithoutExtension(getScriptName(args)))

    if (args.contains("-debugLogging"))
      debugLogging()

    MailUtils.dryRun = if (args.contains("-dryrun")) true else false
    if (MailUtils.dryRun){
      logger.info("Running in debug mode")
    }

    Some(script)
  }

  def main(args: Array[String]) {

    try {
      val script = handleArgs(args)
      if (!script.isDefined){
        println(s"Usage: $usage")
        return
      }

      val result = ScriptHelper.runScript(script.get)
      logger.info(s"finished processing; result: ${result}")
    }
    catch {
      case e: Throwable => logger.error("Fatal Error", e)
    }

    try{
      Tags.save()
      Values.save()

      MailUtils.close()
    } catch {
      case e: Throwable => logger.error("Error during close", e)
    }
  }

}
