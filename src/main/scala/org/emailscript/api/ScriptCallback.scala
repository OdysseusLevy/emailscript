package org.emailscript.api

/**
 * In your script you hand in a function that takes an array of MailMessage
 */
trait ScriptCallback{
  def callback(emails: Array[MailMessage]): Unit
}
