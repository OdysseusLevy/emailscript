package org.emailscript.helpers

import java.io.{Reader}
import javax.script.Bindings

import org.emailscript.api._
import org.emailscript.api.Named

import com.google.common.base.Strings


/**
 * Responsible for pulling in configuration information.
 *
 * Reads in all .yml files in the config directory
 */
object Configuration {

  lazy val configs: Map[String, AnyRef] = loadConfigs
  val AccountsDir = "config"
  val DataDir = "data"

  val DataName = "Data"
  val LoggerName = "logger"
  val HelperName = "Helper"

  val yaml = Yaml(AccountsDir)

  def addObjects(scope: Bindings): Unit = {

    addConfig(scope)

    // Add some helpers

    scope.put(DataName, yaml)
    scope.put(LoggerName, LoggerFactory.getLogger("script"))
    scope.put(HelperName, new Helper(scope))
  }

  private def getName(obj: AnyRef, default: String) = {

    obj match {
      case named: Named if (!Strings.isNullOrEmpty(named.getNickname)) => named.getNickname
      case map: java.util.Map[String, AnyVal] if (map.containsKey(Named.Nickname)) => map.get(Named.Nickname).toString
      case _ =>default
    }
  }

  def getConfigFromReader(defaultName: String, reader: Reader): Option[(String,AnyRef)]={
    val data = yaml.read(reader)
    getConfig(defaultName, data)
  }

  def getConfig(defaultName: String, data: Option[AnyRef]): Option[(String,AnyRef)] = {

    data match {

      case Some(i: Importer) => Some(getName(i, defaultName) -> i.doImport)
      case Some(a: AnyRef) => Some(getName(a, defaultName) -> a)
      case _ => None
    }
  }

  private def loadConfigs(): Map[String, AnyRef] = {
    var result = Map[String, AnyRef]()
    val fileHandler = FileHandler(AccountsDir, Yaml.ymlExtension)

    for( (name, reader) <- fileHandler.getAllReaders(); (finalName, data) <- getConfigFromReader(name, reader)){
      if (result.contains(finalName))
        throw new RuntimeException(s"Found two config beans with the same nickname: ${finalName}; first: $data, second: ${result.get(finalName)}")

      result += (finalName -> data)
    }

    result
  }

  def addConfig(scope: Bindings): Unit = {

    configs.foreach{ case (name: String, config: AnyRef) =>
      scope.put(name, config)
    }

  }

}
