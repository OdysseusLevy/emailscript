package org.emailscript.beans

import java.util.Properties

import com.typesafe.scalalogging.slf4j.Logger
import org.emailscript.{MailMessage, MailUtils, ScriptCallback}
import org.slf4j.LoggerFactory

import scala.beans.BeanProperty

object EmailAccount {

  val logger = Logger(LoggerFactory.getLogger(getClass))

  def apply(bean: EmailAccountBean): EmailAccount = {
    new EmailAccount(bean.host, bean.user, bean.password, bean.nickname, bean.port, bean.smtpHost)
  }
}

class EmailAccountBean extends AccountBean {

  @BeanProperty var host: String = ""
  @BeanProperty var user: String = ""
  @BeanProperty var password: String = ""
  @BeanProperty var port: Int = -1
  @BeanProperty var smtpHost: String = ""
  @BeanProperty var smtpPort: String = "465"
}

object EmailAccountBean {

  def apply(host: String, user: String, password: String, smtpHost: String): EmailAccountBean = {
    val bean = new EmailAccountBean
    bean.setHost(host)
    bean.setUser(user)
    bean.setPassword(password)
    bean.setSmtpHost(smtpHost)

    bean
  }
}
case class EmailAccount(val host: String, val user: String, val password: String, val name: String, val port: Int,
                        smtpHost: String, smtpPort: String = "465") {

  def toSmtpProperties():Properties = {
    val props = new Properties()
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.socketFactory.port", smtpPort);
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.port", smtpPort);
    props
  }

  def newMail() = EmailBean(user)
  def send(message: EmailBean) = MailUtils.sendMessage(this, message)

  def scanFolder(folderName: String, scanner: ScriptCallback): Unit = MailUtils.scanFolder(this, folderName, scanner)
  def readLatest(folderName: String, callback: ScriptCallback): Unit = MailUtils.readLatest(this, folderName, callback)

  def getEmails(): Array[MailMessage] = getEmails("Inbox", 0)
  def getEmails(limit: Int): Array[MailMessage] = getEmails("Inbox", limit)
  def getEmails(folderName: String): Array[MailMessage] = getEmails(folderName, 0)
  def getEmailsAfter(folderName: String, startUID: java.lang.Long): Array[MailMessage] = {
    MailUtils.getEmailsAfter(this, folderName, startUID)
  }

  def getEmails(folderName: String, limit: Int): Array[MailMessage] = MailUtils.getEmails(this, folderName, limit)

  def toBean(): EmailAccountBean = {
    val bean = new EmailAccountBean()
    bean.setHost(host)
    bean.setUser(user)
    bean.setPassword(password)
    bean.setNickname(name)
    bean.setPort(port)
    bean.setSmtpHost(smtpHost)
    bean
  }

}
