package org.emailscript.helpers

import java.io.{Reader, Writer, StringReader, StringWriter}
import java.util.Date

import org.emailscript.api._
import org.scalatest.{FlatSpec, Matchers}
import org.yaml.snakeyaml.nodes.Tag

class TestHandler extends StreamHandler {
  override def getReader(name: String): Option[Reader] = None

  override def getAllReaders(): Array[(String, Reader)] = Array()

  override def getWriter(name: String): Writer = new StringWriter()
}

class YamlTest extends FlatSpec with Matchers {

  val yaml = Yaml(new TestHandler)

  def roundTrip[T](data: AnyRef, tagText: String): T = {

    val tag = new Tag(tagText)
    val writer = new StringWriter()
    yaml.save(data, writer)

    writer.toString should startWith (tag.getValue)

    val reader = new StringReader(writer.toString)
    val result = yaml.read(reader)

    result should not be(None)

    result.get.asInstanceOf[T]
  }

  "WhoBean" should "roundtrip using yaml" in {
    val who = WhoBean("WhoName", "WhoEmail@test.org")
    val whoResult = roundTrip[WhoBean](who, Yaml.WhoTag)
    who should have (
      'name ("WhoName"),
      'email("WhoEmail@test.org"))
  }

  "WhoBean" should "load using yaml" in {
    val text = """!Who
                 |  email: test@gmail.com
                 |  name: Test Name""".stripMargin

    val data = yaml.read(new StringReader(text))
    data match {
      case Some(wb: WhoBean) =>
        wb should have (
        'email ("test@gmail.com"),
        'name ("Test Name")
        )
      case _ => fail(s"Unrecognized result: $data")
    }
  }

  "IndexerBean" should "roundtrip using yaml" in {

    val testUrl = "TestUrl"
    val indexer = new IndexerBean()
    indexer.url = testUrl

    val indexerResult = roundTrip[IndexerBean](indexer, Yaml.IndexerTag)

    indexerResult.url should be (testUrl)
  }

  "IndexerBean" should "load using yaml" in {
    val text =
      """!Indexer
        |  nickname: Indexer
        |  url: http://test.org/test
      """.stripMargin

    val data = yaml.read(new StringReader(text))

    data match {
      case Some(ib: IndexerBean) =>
        ib should have (
        'nickname ("Indexer"),
        'url ("http://test.org/test")
        )
    }
  }

  "LastScan" should "roundtrip using yaml" in {

    val startDate = new Date(123)
    val stopDate = new Date(1234)
    val lastScan = LastScan(startDate, stopDate, 333L)

    val lastScanResult = roundTrip[LastScan](lastScan, Yaml.LastScanTag)
    lastScanResult should have (
      'start (startDate),
      'stop (stopDate),
      'lastId (333L))
  }

  "EmailAccountBean" should "roundtrip using yaml" in {
    val account = EmailAccount.createBean("host1", "user1", "password1", "smtpHost1")

    val accountResult = roundTrip[EmailAccountBean](account, Yaml.EmailAccountTag)
    accountResult should have (
      'imapHost ("host1"),
      'user ("user1"),
      'password ("password1"),
      'smtpHost ("smtpHost1"))
  }

  "EmailAccountBean" should "load from text properly" in {

    val text = """!EmailAccount
      |nickname: Gmail
      |imapHost: imap.gmail.com
      |user: test@gmail.com
      |password: password1""".stripMargin

    val bean = yaml.read(new StringReader(text))

    bean match {
      case Some(ea: EmailAccountBean) => {
        ea should have (
        'nickname ("Gmail"),
        'imapHost ("imap.gmail.com"),
        'user ("test@gmail.com"),
        'password ("password1")
        )
      }
      case _ => fail(s"unexpected result $bean")
    }
  }

  "GoogleContactsBean" should "roundtrip using yaml" in {
    val gContacts = new GoogleContactsBean()
    gContacts.setAccount("account1")
    gContacts.setPassword("password1")

    val gContactsResult = roundTrip[GoogleContactsBean](gContacts, Yaml.GoogleContactsTag)

    gContactsResult should have (
      'account ("account1"),
      'password ("password1"))
  }

  "GoogleContactsBean" should "load from text" in {
    val text = """!GoogleContacts
                 |account: test@gmail.com
                 |password: password2
                 |nickname: GoogleContacts""".stripMargin

    val bean = yaml.read(new StringReader(text))

    bean match {
      case Some(gcb: GoogleContactsBean) =>
        gcb should have (
        'nickname ("GoogleContacts"),
        'account ("test@gmail.com"),
        'password ("password2")
        )
    }
  }
}
