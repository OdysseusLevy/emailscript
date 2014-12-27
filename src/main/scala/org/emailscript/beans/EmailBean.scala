package org.emailscript.beans

import javax.mail.{Session, Message}
import javax.mail.internet.MimeMessage

import scala.beans.BeanProperty

/**
 * Represent a simple email message
 */
class EmailBean {
  @BeanProperty var subject: String = ""
  @BeanProperty var from: Who = Who.NoOne
  @BeanProperty var to: Who = Who.NoOne
  @BeanProperty var html: String = ""

  def setTo(name: String, email: String) = {to = Who(name, email)}

  def setFrom(name: String, email: String) = {from = Who(name,email)}

  def toMessage(session: Session):Message = {
    val message = new MimeMessage(session)

    message.setRecipient(Message.RecipientType.TO, to.toAddress)
    message.setFrom(from.toAddress)
    message.setSubject(subject)
    message.setText(html, "utf-8", "html") //Review -- for now we assume html
    message
  }
}

