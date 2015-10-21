package org.emailscript.api

import javax.mail.Address
import javax.mail.internet.InternetAddress

import org.emailscript.helpers._
import org.emailscript.mail.MailUtils

import scala.beans.BeanProperty

class WhoBean extends Importer {

  override def doImport() = Who(name, email)
  @BeanProperty var name: String = ""
  @BeanProperty var email: String = ""

  /** @group Plumbing */
  override def hashCode() = {email.hashCode}

  /** @group Plumbing */
  override def equals(o: Any): Boolean = {
    o match {
      case s: String => email.equals(s.toLowerCase())
      case who: WhoBean => who.email.equals(email)
      case _ => false
    }
  }
}

object WhoBean {
  def apply(name: String, email: String) = {
    val bean = new WhoBean
    bean.name = name
    bean.email = email
    bean
  }
}
/**
 * Email address and Name of person
 *
 * Found in the from, to, cc, etc. fields of an email
 *
 * ==Features==
 *  - Set multiple tags
 *  - Set named values
 */
class Who(val name: String, val email: String, tags: Tags, values: Values ) extends Exporter {

  def doExport(): WhoBean = WhoBean(name, email)

  def getName(): String = name
  def getEmail(): String = email

  /**
   * Host of email -- eg. hotmail.com, gmail.com, yahoo.com, etc.
   * @group Properties
   */
  def getHost() = Who.getHost(email)

  //
  // Api
  //

  /**
   * Set one value for this user. Subsequent sets will replace the value
   * @param name
   * @param value
   * @group Functions
   */
  def setValue(name: String, value: AnyRef) = values.setValue(doExport(), name, value)

  /**
   * Get the value for this user
   * @param name
   * @return "" if no value is set, otherwise returns the value last set with setValue()
   */
  def getValue(name: String): AnyRef = values.getValue(doExport, name)

  //
  // Tags
  //

  /**
   * Add a tag to the set of tags for this user
   * @param tag
   * @group Functions
   */
  def addTag(tag: String) = tags.setTag(this, tag)

  /**
   * Checks to see if this use has the given tag set
   * @param tag
   * @return
   * @group Functions
   */
  def hasTag(tag: String): Boolean = tags.hasTag(this, tag)

  /**
   * Remove a given tag
   * @param tag
   * @group Functions
   */
  def removeTag(tag: String) = tags.removeTag(this, tag)

  /**
   * Get all tags set for this user
   * @group Functions
   */
  def getTags(): Set[String]= tags.getTags(this)

  /**
   * Determine if the given email is valid
   * @group Functions
   */
  def isValid(): Boolean = MailUtils.isValidEmail(email)

  //
  // Plumbing
  //

  /** @group Plumbing */
  override def hashCode() = {email.hashCode}

  /** @group Plumbing */
  override def equals(o: Any): Boolean = {
    o match {
      case s: String => email.equals(s.toLowerCase())
      case who: Who => who.email.equals(email)
      case _ => false
    }
  }

  /** @group Plumbing */
  override def toString = {s"$name<$email>"}

  /** @group Plumbing */
  def toAddress():Address = new InternetAddress(email, name)
}

object Who {
  val NoOne = Who("None", "None")

  import org.emailscript.mail.MailMessageHelper.getOrElse

  def getHost(email: String): String = {
    val index = email.lastIndexOf('@')
    if (index < 0 || index >= email.size - 1)
      ""
    else
      email.substring(index + 1)

  }

  def apply(name: String, email: String) = {
    val tags = Tags.getTags("who_tags")
    val values = Values.getValues[WhoBean]("who_values")
    new Who(name, email, tags, values)
  }

  def apply(address: Address): Who = {
    val ia = address.asInstanceOf[InternetAddress]
    Who(getOrElse(ia.getPersonal, ""), getOrElse(ia.getAddress, ""))
  }

  def getOne (addressArray: Array[Address]) = {
    if (addressArray == null || addressArray.length == 0) {
      NoOne
    }
    else {
      Who(addressArray(0))
    }
  }

  def getAll (addressArray: Array[Address]): Array[Who] = {
    if (addressArray == null || addressArray.length == 0) {
      Array(NoOne)
    }
    else {
      addressArray.map {Who(_)}
    }
  }
}
