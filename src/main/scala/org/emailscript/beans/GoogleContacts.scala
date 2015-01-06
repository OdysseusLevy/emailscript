package org.emailscript.beans

import java.net.URL
import java.util.Properties

import com.google.gdata.client.contacts.ContactsService
import com.google.gdata.data.PlainTextConstruct
import com.google.gdata.data.contacts._
import com.google.gdata.data.extensions.{Email, FullName, Name}
import org.emailscript.ContactsAccount

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

case class GoogleContact (groups: Set[String], emails: Set[String], title: String, groupEntries: List[GroupMembershipInfo])

object GoogleContact {

  def apply(contactEntry: ContactEntry, groupNames: Map[String, String]) = {

    val groups: Set[String] = contactEntry.getGroupMembershipInfos.map { g => groupNames.getOrElse(g.getHref, g.getHref)}.toSet
    val emails: Set[String] = contactEntry.getEmailAddresses.map { e => e.getAddress.toLowerCase}.toSet

    new GoogleContact(groups, emails, contactEntry.getTitle.getPlainText, contactEntry.getGroupMembershipInfos.toList)
  }
}

case class GoogleContacts(account: String, password:String) extends ContactsAccount {

  lazy val service: ContactsService = GoogleContacts.getService(account, password)
  import org.emailscript.beans.GoogleContacts._

  lazy val groupHrefToName = getGroupMap(service, account)
  lazy val groupNameToHref = groupHrefToName.map { entry => (entry._2, entry._1) }.toMap

  override lazy val contacts: List[GoogleContact] = getContacts(service, account, groupHrefToName)

  override lazy val emails: Set[String] = {

    val entries = for {contact <- contacts; email <- contact.emails} yield email.toLowerCase
    entries.toSet
  }

  override def contains(who: Who): Boolean = contains(who.email)
  override def contains(email: String): Boolean = emails.contains(email.toLowerCase)

  override def addContact(who: Who) = addContact(who, Set())
  override def addContact(who: Who, whoGroups: Set[String]) {
    val postURL = new URL(s"https://www.google.com/m8/feeds/contacts/$account/full")
    val contact = new ContactEntry()

    contact.setTitle(new PlainTextConstruct(who.getName()))
    val name = new Name()
    name.setFullName(new FullName(who.name, ""))

    contact.setName(name)
    val primaryMail = new Email()
    primaryMail.setAddress(who.email)
    primaryMail.setRel("http://schemas.google.com/g/2005#home")
    primaryMail.setPrimary(true)
    contact.addEmailAddress(primaryMail)

    whoGroups.foreach{ name =>
      if (groupNameToHref.contains(name))
        contact.addGroupMembershipInfo(new GroupMembershipInfo(false, groupNameToHref(name)))
    }
    service.insert(postURL, contact)
  }

}

class GoogleContactsBean extends AccountBean {
  @BeanProperty var account: String = ""
  @BeanProperty var password: String = ""
}

object GoogleContactsBean {
  def apply(account: String, password: String): GoogleContactsBean = {
    val bean = new GoogleContactsBean()
    bean.setAccount(account)
    bean.setPassword(password)
    bean
  }
}

object GoogleContacts {

  val appName = "emailscript.org"
  val maxResults = 1200

  def apply(bean: GoogleContactsBean): GoogleContacts = {
    new GoogleContacts(bean.account, bean.password)
  }

  private def getService(account: String, password: String) = {
    val service = new ContactsService(appName)
    service.setUserCredentials(account, password)
    service
  }

  private def getGroupMap(service: ContactsService, account: String): Map[String,String] = {
    val feedUrl = new URL(s"https://www.google.com/m8/feeds/groups/$account/full")
    val groupEntries = service.getFeed(feedUrl, classOf[ContactGroupFeed]).getEntries.toList
    groupEntries.map { g => (g.getId, g.getTitle.getPlainText)}.toMap
  }

  def getContacts(service: ContactsService, account: String, groupNames: Map[String,String]): List[GoogleContact] = {

    val feedUrl = new URL(s"https://www.google.com/m8/feeds/contacts/$account/full?max-results=$maxResults")

    val feed = service.getFeed(feedUrl, classOf[ContactFeed])
    val entries = feed.getEntries.toList
    entries.map { ce => GoogleContact(ce, groupNames)}
  }

  def main(args: Array[String]) {


    //contacts.addContact(Who("Testwww", "testvvvv@test.com"), Set("Businesses", "Family"))

//    contacts.groupNames.foreach { name: String =>
//      println(name)
//    }
//
//    println(contacts.emails.mkString(","))
//    contacts.contacts.foreach { contact: GoogleContact =>
//      println(contact.title)
//      println("\tgroups: " + contact.groups.mkString(","))
//      println("\tgroup info:" + contact.groupEntries.mkString(","))
//      println("\temails: " + contact.emails.mkString(","))
//    }

  }
}
