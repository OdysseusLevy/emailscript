package org.emailscript.helpers

import org.emailscript.api._
import org.scalatest.{FlatSpec, Matchers}

class ConfigurationTest extends FlatSpec with Matchers {

  "IndexerBean" should "produce a named Indexer obj" in {

    val bean = new IndexerBean()
    bean.setUrl("url1")
    bean.setNickname("name1")

    val result = Configuration.getConfig("", Some(bean))

    result match {
      case Some((name:String, indexer:Indexer)) => {
        name should be ("name1")
        indexer.url should startWith ("url1")
      }
      case _ => fail(s"unexpected configuration: $result")
    }

  }

  "GoogleContactsBean" should "produce a GoogleContacts" in {

    val bean = new GoogleContactsBean
    bean.setNickname("name2")
    bean.setAccount("account1")
    bean.setPassword("password1")

    val result = Configuration.getConfig("", Some(bean))

    result match {
      case Some((name: String, gc: GoogleContacts)) => {
        name should be ("name2")
        gc.account should be ("account1")
        gc.password should be ("password1")
      }
      case _ => fail(s"Unrecognized result: $result")
    }
  }

  "EmailAccountBean" should "produce an EmailAccount" in {

    val bean = new EmailAccountBean()
    bean.setNickname("name3")
    bean.setImapHost("imap1")
    bean.setImapPort(2)
    bean.setUser("user1")
    bean.setPassword("password1")
    bean.setSmtpHost("smpt1")
    bean.setSmtpPort(3)

    val result = Configuration.getConfig("", Some(bean))

    result match {
      case Some((name: String, ea: EmailAccount)) => {
        name should be("name3")
      }
      case _ => fail(s"Unknown recoginized result: $result")
    }
  }
}
