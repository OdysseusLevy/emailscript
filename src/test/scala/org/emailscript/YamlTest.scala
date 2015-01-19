package org.emailscript

import java.io.{StringReader, StringWriter}
import java.util.Date

import org.emailscript.api.{GoogleContacts, LastScan, Who, EmailAccount}
import org.emailscript.helpers.Yaml
import org.scalatest.{Matchers, FlatSpec}
import org.yaml.snakeyaml.nodes.Tag

class YamlTest extends FlatSpec with Matchers {

  def roundTrip[T](data: AnyRef, tag: Tag): T = {

    val writer = new StringWriter()
    Yaml.save(data, writer)
    println(writer.toString)

    writer.toString should startWith (tag.getValue)

    val reader = new StringReader(writer.toString)
    Yaml.read(reader).get.asInstanceOf[T]
  }

  "Yaml Object" should "round trip values of type Who, EmailAccountApi, GoogleContacts, LastScan" in {

    val who = Who("WhoName", "WhoEmail@test.org")
    val account = EmailAccount("host1", "user1", "password1", "smtpHost1")

    account should have (
      'imapHost ("host1"),
      'user ("user1"),
      'password ("password1"),
      'smtpHost ("smtpHost1"))

    val gContacts = GoogleContacts("account1", "password1")

    val startDate = new Date(123)
    val stopDate = new Date(1234)
    val lastScan = LastScan(startDate, stopDate, 333L)

    val whoResult = roundTrip[Who](who, Yaml.WhoTag)
    val accountResult = roundTrip[EmailAccount](account, Yaml.EmailAccountTag)
    val gContactsResult = roundTrip[GoogleContacts](gContacts, Yaml.GoogleContactsTag)
    val lastScanResult = roundTrip[LastScan](lastScan, Yaml.LastScanTag)

    who should be (whoResult)

    accountResult should have (
      'imapHost ("host1"),
      'user ("user1"),
      'password ("password1"),
      'smtpHost ("smtpHost1"))

    gContactsResult should have (
      'account ("account1"),
      'password ("password1"))

    lastScanResult should have (
      'start (startDate),
      'stop (stopDate),
      'lastId (333L))
  }
}
