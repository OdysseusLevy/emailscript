package org.emailscript.helpers

import java.io.{File, FileReader}
import java.util.Properties
import javax.script.Bindings

import org.emailscript.api._
import com.google.common.base.Strings
import com.google.common.io.Files
import org.slf4j.LoggerFactory


/**
 * Responsible for pulling in configuration information.
 *
 * Looks for files ending with *.imap for imap email accounts
 */
object Configuration {

  val Accounts = "config"
  val DataName = "Data"
  val LoggerName = "logger"
  val HelperName = "Helper"
  val SearchName = "Search"

  val homeDir = new File(".")

  def getFiles(folderName: String, suffix: String): Stream[File] = {
    val folder = new File(folderName)
    if (!folder.exists()){
      folder.mkdir()
    }

    folder.listFiles.toStream.filter(_.getName.endsWith(suffix))
  }

  def stripSuffix(name: String) = {
    val index = name.lastIndexOf(".")
    if (index < 0)
      name
    else
      name.substring(0, index)
  }

  def getProperties(file: File): Properties = {
    val props = new Properties()
    props.load(new FileReader(file))
    props
  }

  def addObjects(scope: Bindings): Unit = {

    addConfig(scope)

    // Add some helpers

    scope.put(DataName, Yaml())
    scope.put(LoggerName, LoggerFactory.getLogger("script"))
    scope.put(HelperName, new Helper(scope))

    scope.put(SearchName, new Search(homeDir))
  }

  private def readBean(file: File): Option[(String,AnyRef)]= {

    val NickName = "nickname"

    val data = Yaml.readFromFile(file)
    data match {

      case Some(namedBean: NamedBean) => {
        if (Strings.isNullOrEmpty(namedBean.getNickname))
          namedBean.setNickname(Files.getNameWithoutExtension(file.getName))
        Option(namedBean.getNickname -> namedBean)
      }
      case Some(map: java.util.Map[String, AnyRef]) if (map.containsKey(NickName)) =>
        val nickName = map.get(NickName)
        if (nickName == null || nickName.toString == "")
          map.put(NickName, Files.getNameWithoutExtension(file.getName))
        Option(map.get(NickName).toString -> map)

      case _ => None
    }
  }

  def addConfig(scope: Bindings): Unit = {

    getFiles(Accounts, ".yml").flatMap(readBean(_)).foreach { case (name: String, value: AnyRef) =>

      if (scope.get(name) != null)
        throw new RuntimeException(s"Found two config beans with the same nickname: ${name}; first: $value, second: ${scope.get(name)}")

      scope.put(name, value)
    }
  }

}
