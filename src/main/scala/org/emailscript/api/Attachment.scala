package org.emailscript.api

import javax.mail.Part

/**
 * Represents email attachments
 */
case class Attachment(part: Part) {

  def getFileName() = part.getFileName
  def getInputStream() = part.getInputStream
}
