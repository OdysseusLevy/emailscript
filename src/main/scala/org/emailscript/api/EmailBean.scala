package org.emailscript.api

import javax.mail.internet.MimeMessage
import javax.mail.{Message, Session}

import org.emailscript.mail.MailUtils

import scala.beans.BeanProperty

/**
 * Use this to send very simple email messages
 */
class EmailBean {
  @BeanProperty var subject: String = ""
  @BeanProperty var from: Who = Who.NoOne
  @BeanProperty var to: Who = Who.NoOne
  @BeanProperty var folder: String = ""
  @BeanProperty var body: String = ""
  @BeanProperty var uid: Long = -1
  @BeanProperty var mimeSubType: String = "html"

  def setHtml(text: String): Unit = {
    body = text
    mimeSubType = "html"
  }

  def setText(text: String): Unit = {
    body = text
    mimeSubType = "plain"
  }

  def setTo(email: String) = {to = Who("", email)}
  def setTo(name: String, email: String) = {to = Who(name, email)}

  def setFrom(name: String, email: String) = {from = Who(name,email)}

  def toMessage(session: Session):Message = {
    val message = new MimeMessage(session)

    message.setRecipient(Message.RecipientType.TO, to.toAddress)
    message.setFrom(from.toAddress)
    message.setSubject(subject)
    message.setText(body, "utf-8", mimeSubType)
    message
  }
}

object EmailBean {
  def apply(user: String) = {
    val bean = new EmailBean()
    if (MailUtils.isValidEmail(user)) {
      bean.to = Who("", user)
      bean.from = Who("", user)
    }
    bean
  }
}

