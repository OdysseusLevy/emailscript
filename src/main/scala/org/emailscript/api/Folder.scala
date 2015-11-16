package org.emailscript.api

import com.sun.mail.imap.IMAPFolder

/**
  * Simplified IMAP Folder
  */
class Folder(folder: IMAPFolder) {

  /**
    * Name of this folder
    * eg "Trash"
    */
  def getName(): String = folder.getName

  /**
    * Fully qualified name, so it includes parents as well.
    * Eg. [Gmail]/Trash
    */
  def getFullName(): String = folder.getFullName

  /**
    * Is this the type of folder that can contain messages?
    * For example, Google's [GMail] folder returns false
    */
  def getHoldsMessages(): Boolean = {
    (folder.getType & javax.mail.Folder.HOLDS_MESSAGES) != 0
  }

  /**
    * Is this the type of folder than contains folders (and not messages)?
    * For example, Google's [Gmail] folder returns true
    * @return
    */
  def getHoldsFolders(): Boolean = {
    (folder.getType & javax.mail.Folder.HOLDS_FOLDERS) != 0
  }

  def getFolders(): Array[Folder] = {
    folder.list("*").map(f => new Folder(f.asInstanceOf[IMAPFolder]))
  }

}

object Folder {

  def apply(folder: IMAPFolder) = new Folder(folder)
}
