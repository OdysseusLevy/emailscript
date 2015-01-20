package org.emailscript.api

import javax.mail.Address
import javax.mail.internet.InternetAddress

import org.emailscript.helpers.{ValuesMap, Tags, Values}
import org.emailscript.mail.MailUtils

import scala.beans.BeanProperty

/**
 * Email address and Name of person
 *
 * Found in the from, to, cc, etc. fields of an email
 *
 * ==Features==
 *  - Set multiple tags
 *  - Set named values
 */
class Who extends ValuesImmutableBean {

  //
  // Bean interface
  //

  /**
   * Name -- eg. Odysseus Levy
   * @group Properties
   */
  def setName(name: String): Unit = set('Name, name)
  /** @group Properties */
  def getName(): String = getOrElse('Name, "")

  /**
   * Email
   * @group Properties
   */
  def setEmail(email: String): Unit = set('Email, email)
  /** @group Properties */
  def getEmail(): String = getOrElse('Email, "")

  /**
   * Host of email -- eg. hotmail.com, gmail.com, yahoo.com, etc.
   * @group Properties
   */
  def getHost() = Who.getHost(getEmail)

  //
  // Api
  //

  /**
   * Set one value for this user. Subsequent sets will replace the value
   * @param name
   * @param value
   * @group Functions
   */
  def setValue(name: String, value: AnyRef) = Values.setValue(this, name, value)

  /**
   * Get the value for this user
   * @param name
   * @return "" if no value is set, otherwise returns the value last set with setValue()
   */
  def getValue(name: String): AnyRef = Values.getValue(this, name)

  //
  // Tags
  //

  /**
   * Add a tag to the set of tags for this user
   * @param tag
   * @group Functions
   */
  def addTag(tag: String) = Tags.setTag(this, tag)

  /**
   * Checks to see if this use has the given tag set
   * @param tag
   * @return
   * @group Functions
   */
  def hasTag(tag: String): Boolean = Tags.hasTag(this, tag)

  /**
   * Remove a given tag
   * @param tag
   * @group Functions
   */
  def removeTag(tag: String) = Tags.removeTag(this, tag)

  /**
   * Get all tags set for this user
   * @group Functions
   */
  def getTags(): Set[String]= Tags.getTags(this)

  /**
   * Determine if the given email is valid
   * @group Functions
   */
  def isValid(): Boolean = MailUtils.isValidEmail(getEmail)

  //
  // Plumbing
  //

  /** @group Plumbing */
  override def hashCode() = {getEmail.hashCode}

  /** @group Plumbing */
  override def equals(o: Any): Boolean = {
    o match {
      case s: String => getEmail.equals(s.toLowerCase())
      case who: Who => who.getEmail.equals(getEmail)
      case _ => false
    }
  }

  /** @group Plumbing */
  override def toString = {s"$getName<$getEmail>"}

  /** @group Plumbing */
  def toAddress():Address = new InternetAddress(getEmail, getName)
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
    val who = new Who()
    who.setEmail(email)
    who.setName(name)
    who
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
