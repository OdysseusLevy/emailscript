package org.emailscript

import java.io.File

import com.google.common.io.Files
import org.emailscript.helpers._
import org.emailscript.mail.MailUtils


object Emailscript {

  val logger = LoggerFactory.getLogger(getClass)
  val usage = "[-h | --help] [-debugLogging] [-dryrun] scriptname"


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
    // Configure the logger used by our script to output to <scriptName>.log (see logback.xml config file)
    //
    Logger.setScriptLogName(Files.getNameWithoutExtension(getScriptName(args)))

    if (args.contains("-debugLogging"))
      Logger.debugLogging()

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
