package org.emailscript

import java.io.{StringReader, StringWriter}
import java.util.Date

import org.emailscript.beans._
import org.scalatest.{Matchers, FlatSpec}
import org.yaml.snakeyaml.nodes.Tag

class YamlTest extends FlatSpec with Matchers {

  def roundTrip[T](data: AnyRef, tag: Tag): T = {

    val writer = new StringWriter()
    Yaml.save(data, writer)
    println(writer.toString)

    writer.toString should startWith (tag.getValue)

    val reader = new StringReader(writer.toString)
    Yaml.read[T](reader).get
  }

  "Yaml Object" should "round trip values of type Who, EmailAccount, GoogleContacts, LastScan" in {

    val who = Who("WhoName", "WhoEmail@test.org")
    val account = EmailAccountBean("host1", "user1", "password1", "smtpHost1")
    val gContacts = GoogleContactsBean("account1", "password1")

    val startDate = new Date()
    val stopDate = new Date(startDate.getTime + 2000)

    val lastScan = LastScan(startDate, stopDate, 333L)

    val whoResult = roundTrip[Who](who, Yaml.WhoTag)
    val accountResult = roundTrip[EmailAccountBean](account, Yaml.EmailAccountTag)
    val gContactsResult = roundTrip[GoogleContactsBean](gContacts, Yaml.GoogleContactsTag)
    val lastscCanResult = roundTrip[LastScan](lastScan, Yaml.LastScanTag)

    who should be (whoResult)

    accountResult should have (
      'host ("host1"),
      'user ("user1"),
      'password ("password1"),
      'smtpHost ("smtpHost1"))

    gContactsResult should have (
      'account ("account1"),
      'password ("password1"))

    lastscCanResult should have (
      'start (startDate),
      'stop (stopDate),
      'lastId (333L))
  }
}
