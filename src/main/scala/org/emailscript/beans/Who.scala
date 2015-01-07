package org.emailscript.beans

import javax.mail.{Address}
import javax.mail.internet.InternetAddress
import org.emailscript.{MailMessage, Values, Tags}

import scala.beans.BeanProperty

/**
 * Helper class for dealing with javamail email addresses
 *
 */
class Who {

  @BeanProperty var name:String = ""
  var email:String = ""
  var host: String = ""

  def setEmail(email: String) = {
    this.email = email.toLowerCase
    host = Who.getHost(this.email)
  }

  def getEmail() = email

  //
  // Values
  //

  def setValue(name: String, value: AnyRef) = Values.setValue(this, name, value)
  def getValue(name: String): AnyRef = Values.getValue(this, name)

  //
  // Tags
  //

  def setTag(tag: String) = Tags.setTag(this, tag)
  def hasTag(tag: String): Boolean = Tags.hasTag(this, tag)
  def removeTag(tag: String) = Tags.removeTag(this, tag)
  def getTags(): Set[String]= Tags.getTags(this)

  def isValid(): Boolean = {
    try{
      toAddress().validate()
      return true
    } catch {
      case e: Throwable => return false
    }
  }

  override def hashCode() = {email.hashCode}
  override def equals(o: Any): Boolean = {
    o match {
      case s: String => email.equals(s.toLowerCase())
      case who: Who => who.email.equals(email)
      case _ => false
    }
  }

  override def toString = {s"$name<$email>"}

  def toAddress():InternetAddress = new InternetAddress(email, name)
}

object Who {
  val NoOne = Who("None", "None")

  import MailMessage.getOrElse

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
