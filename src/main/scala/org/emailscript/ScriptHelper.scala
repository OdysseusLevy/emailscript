package org.emailscript

import java.io.{File, FileReader}
import javax.script.ScriptEngineManager

import com.google.common.io.Files
import org.cosmosgame.mailscript.Configuration
import org.slf4j.LoggerFactory

/**
 * Run scripts
 *
 * We currently support groovy, javascript (Nashorn), and ruby
 */
object ScriptHelper {
  val logger = LoggerFactory.getLogger(getClass)

  def getEngineName(f: File): String = {
    Files.getFileExtension(f.getName) match {
      case "ruby" => "ruby"
      case "groovy" => "groovy"
      case _ => "nashorn"
    }
  }

  def runScript(script: File): Object = {
    val engineName = getEngineName(script)

    logger.info(s"Executing ${script.getName} with engine ${engineName}")

    val engineManager = new ScriptEngineManager();
    val engine = engineManager.getEngineByName(engineName);
    val scope = engine.createBindings()

    Configuration.addObjects(scope)
    val reader = new FileReader(script)
    engine.eval(reader, scope)
  }
}
