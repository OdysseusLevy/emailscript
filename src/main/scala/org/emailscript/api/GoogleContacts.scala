package org.emailscript.api

import java.net.URL

import com.google.gdata.client.contacts.ContactsService
import com.google.gdata.data.PlainTextConstruct
import com.google.gdata.data.contacts.{GroupMembershipInfo, ContactEntry, ContactFeed, ContactGroupFeed}
import com.google.gdata.data.extensions.{Email => GoogleEmail, FullName, Name}
import org.emailscript.helpers.Importer
import org.emailscript.helpers.LoggerFactory
import scala.beans.BeanProperty
import scala.collection.JavaConverters._

/**
 * Used to configure a GoogleContacts account
 */
class GoogleContactsBean extends NamedBean with Importer {

  def doImport() = GoogleContacts(account, password)
  /**
   * user account, typically your email eg; myname@gmail.com
   * @group Properties
   */
  @BeanProperty var account: String = ""

  /**
   * user password
   * @group Properties
   */
  @BeanProperty var password: String = ""
}

/**
 * Simple GoogleContacts api.
 * Only works with Google Contacts. Does not yet work with Google+ circles
 *
 * ==Features==
 *
 *  - Get all of your google contacts
 *  - Determine what groups your contact belongs to
 *  - Add new contacts
 *
 * ==Configuration==
 *
 * When running from the command line you set up access to your google account by adding a .yml file to the config
 * directory
 * {{{
 * !GoogleContacts
 * account: <myaccount@gmail.com>
 * password: <mypassword>
 * }}}
 *
 */

class GoogleContacts(val account: String, val password: String) {

  import GoogleContacts._

  private lazy val service: ContactsService = GoogleContacts.getService(account, password)
  private lazy val groupHrefToName = getGroupMap(service, account)
  private lazy val groupNameToHref = groupHrefToName.map { entry => (entry._2, entry._1) }.toMap
  private lazy val contacts: List[GoogleContact] = loadContacts(service, account, groupHrefToName)
  private lazy val emails: Set[String] = {
    val entries = for {contact <- contacts; email <- contact.emails.asScala} yield email.toLowerCase
    entries.toSet
  }

  /**
   * Check if one of contacts has the same email as this user
   * @param who
   * @return true if the GoogleContacts has a matching email for this user
   * @group Functions
   */
  def contains(who: Who): Boolean = contains(who.getEmail)

  /**
   * Check if one of the contacts has this email
   * @param email
   * @return true if the GoogleContacts has a matching email for this user
   * @group Functions
   */
  def contains(email: String): Boolean = emails.contains(email.toLowerCase)

  /**
   * Add a contact to Google Contacts
   * @param who
   * @group Functions
   */
  def addContact(who: Who): Unit = addContact(who, Set())

  /**
   * Add a contact to Google Contacts and put them into some groups
   * @param who
   * @param whoGroups
   * @group Functions
   */
  def addContact(who: Who, whoGroups: Set[String]) {
    val postURL = new URL(s"$GoogleApiUrl/contacts/${account}/full")
    val contact = new ContactEntry()

    contact.setTitle(new PlainTextConstruct(who.getName))
    val name = new Name()
    name.setFullName(new FullName(who.getName, ""))

    contact.setName(name)
    val primaryMail = new GoogleEmail()
    primaryMail.setAddress(who.getEmail)
    primaryMail.setRel("http://schemas.google.com/g/2005#home")
    primaryMail.setPrimary(true)
    contact.addEmailAddress(primaryMail)

    whoGroups.foreach{ name =>
      if (groupNameToHref.contains(name))
        contact.addGroupMembershipInfo(new GroupMembershipInfo(false, groupNameToHref(name)))
    }
    service.insert(postURL, contact)
  }

  def getContacts(): java.util.List[GoogleContact] = contacts.asJava

  /**
   * For every email in this contacts account return a "Who" object
   * @return emails wrapped in a Who object (so we can know the name)
   */
  def getEmails(): java.util.List[Who] = {
    val whos = for (contact <- contacts; email <- contact.getEmails.asScala) yield (Who(contact.name, email))
    whos.asJava
  }
}

object GoogleContacts {
  val logger = LoggerFactory.getLogger(getClass)

  val AppName = "emailscript.org"
  val MaxResults = 2000
  val GoogleApiUrl = "https://www.google.com/m8/feeds"

  def apply(account: String, password: String): GoogleContacts = {
   new GoogleContacts(account, password)
  }

  private def getService(account: String, password: String) = {
    val service = new ContactsService(AppName)
    service.setUserCredentials(account, password)
    service
  }

  private def getGroupMap(service: ContactsService, account: String): Map[String, String] = {
    val feedUrl = new URL(s"$GoogleApiUrl/groups/$account/full")
    val groupEntries = service.getFeed(feedUrl, classOf[ContactGroupFeed]).getEntries.asScala
    groupEntries.map { g => (g.getId, g.getTitle.getPlainText)}.toMap
  }

  def loadContacts(service: ContactsService, account: String, groupNames: Map[String, String]): List[GoogleContact] = {

    val url = s"$GoogleApiUrl/contacts/$account/full?max-results=$MaxResults"
    logger.info(s"contacting google with url: $url")
    val feedUrl = new URL(url)

    val feed = service.getFeed(feedUrl, classOf[ContactFeed])
    val entries = feed.getEntries.asScala
    entries.map { ce => GoogleContact(ce, groupNames)}.toList
  }

  /**
   * Represents a Google contact
   *
   * @param emails emails this contact has
   * @param name   name of contact
   * @param groups  groups this contact is member of
   */
  class GoogleContact (
                        @BeanProperty val emails: java.util.Set[String],
                        @BeanProperty val name: String,
                        @BeanProperty val groups: java.util.Set[String],
                        private val groupEntries: Iterable[GroupMembershipInfo]) {

    override def toString() = s"name: $name emails: ${emails.toString}, groups: ${groups.toString}"
  }

  object GoogleContact {

    def apply(contactEntry: ContactEntry, groupNames: Map[String, String]) = {

      val groups: Set[String] = contactEntry.getGroupMembershipInfos.asScala.map { g => groupNames.getOrElse(g.getHref, g.getHref)}.toSet
      val emails: Set[String] = contactEntry.getEmailAddresses.asScala.map { e => e.getAddress.toLowerCase}.toSet

      new GoogleContact(emails.asJava, contactEntry.getTitle.getPlainText, groups.asJava, contactEntry.getGroupMembershipInfos.asScala)
    }
  }
}
