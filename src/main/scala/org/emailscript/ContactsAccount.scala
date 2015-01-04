package org.emailscript

import org.emailscript.beans.{GoogleContact, Who}

trait ContactsAccount {
  val account: String

  val contacts: List[GoogleContact]
  val emails: Set[String]

  def contains( email:Who): Boolean
  def contains( email: String): Boolean
  def addContact(who: Who): Unit
  def addContact(who: Who, whoGroups: Set[String]): Unit
}