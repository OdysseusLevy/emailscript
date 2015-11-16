package org.emailscript.mail

import java.io.InputStream
import javax.mail.{Multipart, Part}

import com.google.gdata.util.common.base.StringUtil
import org.emailscript.api.Attachment
import org.emailscript.helpers.LoggerFactory

/**
 * Code involving working with JavaMail's Mime structure
 */
object MimePart {

  val logger = LoggerFactory.getLogger(getClass)

  // We want just the content type string, remove everything after the ";" (typically the mime border info)
  def getContentType(part: Part) = StringUtil.makeSafe(part.getContentType()).split(";")(0)

  def dumpStructure(part: Part, prefix: String = ""): Unit ={


    if (part.isMimeType("multipart/*")){
      logger.info(s"$prefix${getContentType(part)}")

      val multi = part.getContent.asInstanceOf[Multipart]
      for(i <- 0 until multi.getCount ) {
        val mp = multi.getBodyPart(i)
        dumpStructure(mp, prefix + "\t")
      }
    }else if (part.isMimeType("text/*")) {
      logger.info(s"$prefix${getContentType(part)}")
    } else {
      logger.info(s"$prefix${getContentType(part)} file: ${part.getFileName} " +
        s"disposition: ${StringUtil.makeSafe(part.getDisposition)}")
    }
  }

  def isAttachment(part: Part): Boolean = {
    Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition)
  }

  def getAttachments(part: Part): List[Attachment] = {

    part.getContent match {
      case multi: Multipart => {

        var list = List[Attachment]()
        for(i <- 0 until multi.getCount) {
          list = list ::: getAttachments(multi.getBodyPart(i))
        }

        list
      }

      case is: InputStream if isAttachment(part) => List(Attachment(part))
      case _ => List()

    }

  }

  def getMultiPartText(multi: Multipart): Option[String] = {

    for(i <- 0 until multi.getCount) {
      val part = multi.getBodyPart(i)

      if (part.isMimeType("text/*"))
        return Option(part.getContent.toString)

      val result = getBodyText(part)
      if (result.isDefined)
        return result
    }

    None
  }

  def getRelatedText(multi: Multipart): Option[String] = {

    var text = ""

    for(i <- 0 until multi.getCount) {
      val part = multi.getBodyPart(i)

      val result = getBodyText(part)
      if (result.isDefined)
        text += result.get
    }

    if (text != "")
      Some(text)
    else
      None
  }

  /**
   * Simplified algorithm for extracting a message's text. When presented with alternatives it will always
   * choose the 'preferred' format (by convention this is html)
   *
   * Note that with complicated formats (such as lots of attachments with text interspersed between them) it will
   * simply return the first text block it finds
   *
   * @return null if no text is found
   */
  def getBodyText(part: Part): Option[String] = {

    part.getContent match {
      case text: String => Option(text)
      case altern: Multipart if (part.isMimeType("multipart/alternative")) =>
        getBodyText(altern.getBodyPart(altern.getCount -1)) // the last one is the 'preferred' version

      case related: Multipart if (part.isMimeType("multipart/related")) => getRelatedText(related)
      case multi: Multipart => getMultiPartText(multi)

      case is: InputStream if isAttachment(part) => Some(s"Attachment[${part.getFileName}]")
      case _ => None
    }
  }
}
