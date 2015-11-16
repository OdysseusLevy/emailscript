package org.emailscript.api

/**
 * In your script you hand in a function that takes an email
 */
trait ProcessCallback {
  def callback(email: Email): Unit
}
