package org.emailscript

import org.emailscript.mail.MailUtils
import org.scalatest.{Matchers, FlatSpec}

class MailUtilsTest extends FlatSpec with Matchers  {

  "isValidEmail" should "validate email syntax" in {

    MailUtils.isValidEmail("test@test.com") should be (true)
    MailUtils.isValidEmail("test.test2@test2.test.com") should be (true)
    MailUtils.isValidEmail("test") should be (false)
    MailUtils.isValidEmail("") should be (false)
    MailUtils.isValidEmail("test@") should be (false)
  }

}
