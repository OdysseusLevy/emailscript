package org.cosmosgame.mailscript

import java.io.{FileReader, File}
import java.util.Properties
import javax.script.Bindings

import com.google.common.base.Strings
import com.google.common.io.Files
import org.emailscript.{Template, Yaml}
import org.emailscript.beans._
import org.slf4j.LoggerFactory

/**
 * Responsible for pulling in configuration information.
 *
 * Looks for files ending with *.imap for imap email accounts
 */
object Configuration {

  val Accounts = "accounts"
  val DataName = "Data"
  val LoggerName = "logger"
  val TemplateName = "Template"

  def getFiles(folder: String, suffix: String): Stream[File] = {
    new File(folder).listFiles.toStream.filter(_.getName.endsWith(suffix))
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

    addAccounts(scope)
    addData(scope)
    scope.put(TemplateName, Template)

    val logger = LoggerFactory.getLogger("script")
    scope.put(LoggerName, logger)
  }

  private def readBean(file: File)= {
    val bean = Yaml.readFromFile[AccountBean](file)
    if (Strings.isNullOrEmpty(bean.getNickname()))
      bean.setNickname(Files.getNameWithoutExtension(file.getName))
    bean
  }

  def addAccounts(scope: Bindings): Unit = {

    val accounts = getFiles(Accounts, ".yml").map(readBean(_)).foreach {bean =>
      val account = bean match {
        case email: EmailAccountBean => EmailAccount(email)
        case google: GoogleContactsBean => GoogleContacts(google)
      }

      if (scope.get(bean.nickname) != null)
        throw new RuntimeException(s"Found two accounts with the same nickname: ${bean.nickname}; first: $account, second: ${scope.get(bean.nickname)}")

      scope.put(bean.nickname, account)
    }
  }

  def addData(scope: Bindings): Unit = {
    scope.put(DataName, Yaml())
  }
}
