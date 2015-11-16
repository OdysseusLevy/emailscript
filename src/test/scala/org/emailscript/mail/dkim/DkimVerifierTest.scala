package org.emailscript.dkim

import java.io.ByteArrayInputStream

import org.scalatest.{FlatSpec, Matchers}

object DkimVerifierTest {

  val keys = Seq("Return-Path", "DKIM-Signature", "DomainKey-Signature", "Received", "MIME-Version",
    "Content-Type", "Content-Transfer-Encoding", "Date" ,"To", "From", "Reply-To", "Subject", "Feedback-ID",
    "List-Unsubscribe", "Message-ID", "Test-Header").map(_.toLowerCase)


  val rawHeaders = Seq(
    """Return-Path: RapidRewards@luv.southwest.com""",
    """Received: from omp.luv.southwest.com ([12.130.137.222]) by mx.perfora.net
    |  (mxeueus001) with ESMTP (Nemesis) id 0MKagL-1Yvjoe1Slz-0025xl for
    |  <odysseus@cosmosgame.org>; Mon, 18 May 2015 16:33:27 +0200""".stripMargin,

    """DKIM-Signature: v=2; a=rsa-sha1; c=relaxed/relaxed; s=southwest; d=luv.southwest.com;
      |  h=MIME-Version:Content-Type:Content-Transfer-Encoding:Date:To:From:Reply-To:Subject:List-Unsubscribe:Message-ID; i=RapidRewards@luv.southwest.com;
      |  bh=iYDR0YLgGmEaV2kWo/y4JNKl5jg=;
      |  b=flrxE6btcfXlUql85qVNmtaf6kGNc8rCWh6f14bsuOF3XgcIwN3ltxMLKNucreMBt9Cj/jYARFNk
      |  FF5g06oFZDv2UvQbRraNdLLHySPWlb+f1sjSFHRN0lxEH4uFXnl78JgAloKCcZkctdqupuQSN79n
      |  ubNV8VrHT4CWFwPA1Uk=""".stripMargin,

    """DKIM-Signature: v=1; a=rsa-sha1; c=relaxed/relaxed; s=southwest; d=luv.southwest.com;
    |  h=MIME-Version:Content-Type:Content-Transfer-Encoding:Date:To:From:Reply-To:Subject:List-Unsubscribe:Message-ID; i=RapidRewards@luv.southwest.com;
    |  bh=iYDR0YLgGmEaV2kWo/y4JNKl5jg=;
    |  b=flrxE6btcfXlUql85qVNmtaf6kGNc8rCWh6f14bsuOF3XgcIwN3ltxMLKNucreMBt9Cj/jYARFNk
    |  FF5g06oFZDv2UvQbRraNdLLHySPWlb+f1sjSFHRN0lxEH4uFXnl78JgAloKCcZkctdqupuQSN79n
    |  ubNV8VrHT4CWFwPA1Uk=""".stripMargin,

    """ DomainKey-Signature: a=rsa-sha1; c=nofws; q=dns; s=southwest; d=luv.southwest.com;
   |  b=c1LVlOyeigO1Y0x2dndOznA6TCBRhv2aIxw/Cw9mt5DvEauNpFgybL7qBdX/PLr8g0BCl1nVNQGv
   |  oSa2ix+uoadlEsfyxhXXsRnqjKcllxz6Dz18XcCodiK0uka7v9x+2zkUmqHeKUNJgApfjwF3tljf
   |    Udc0da4rgtkjkVpPavg=;""".stripMargin,

    """Received: by omp.luv.southwest.com id hb7s3e161vk1 for <odysseus@cosmosgame.org>; Mon, 18 May 2015 07:33:26 -0700 (envelope-from <RapidRewards@luv.southwest.com>)""",
    """MIME-Version: 1.0""",
    """Content-Type: text/html;
    |  charset="UTF-8"""".stripMargin,
    """Content-Transfer-Encoding: quoted-printable""",
    """Date: Mon, 18 May 2015 07:33:26 -0700""",
    """To: odysseus@cosmosgame.org""",
    """From: "Southwest Airlines Rapid Rewards" <RapidRewards@luv.southwest.com>""",
    """Reply-To: "Southwest Airlines Rapid Rewards" <reply@luv.southwest.com>""",
    """Subject: Introducing a new way to earn points""",
    """Feedback-ID: swair.780342:188362:rsysoracle""",
    """List-Unsubscribe: <https://luv.southwest.com/pub/optout/UnsubscribeOneStepConfirmAction?YES=true&_ri_=X0Gzc2X%3DWQpglLjHJlTQGzbbIMJhIYtcLjzgB1g3FeN3rzaO8vzfuuy7t7HA&_ei_=EolaGGF4SNMvxFF7KucKuWO9tFbjSiFCtmbc1pqbO0iBmlmftlUl1SdGIvsYA8MMnYDBhIiN53GHyhQZBDVAzis>, <mailto:unsubscribe-WQpglLjHJlTQGzbbIMJhIYtcLjzgB1g3FeN3rzaO8vzfuuy7t7HA@imh.rsys2.com?subject=List-Unsubscribe>""",
    """Message-ID: <0.1.76.2BF.1D091779A3932EC.0@omp.luv.southwest.com>""",

    """Test-Header: "Southwest Airlines Rapid Rewards" <RapidRewards@luv.southwest.com>""",
    """Test-Header: "Southwest Airlines Rapid Rewards" <attack@spammer.com>""")

  val dnsRecord = """ "g=*; k=rsa; n=" "Contact" "postmaster@responsys.com" "with" "any" "questions" "concerning" "this" "signing" "; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCWpXbZNG3wCg0a53l/yaZrT0G9CPR4GGERv31N9SPke+9fUUtEU6G5k0ApW7udZxNd9n6ZksNRfiwl8M49lfMjAKDbe3lY1nSWzrwsGxLNe/Z2YvJlkPz6i0n8rH34xhknOeY9/rhFm4ciOXMwsbWtGMc6tZgAsb3eRmu0prSF9QIDAQAB;""""
}

class DkimVerifierTest extends FlatSpec with Matchers {

  import DkimVerifierTest._

  "mapHeaderFields" should "read all headers into stacks" in {

    val stacks = DkimVerifier.buildHeaderStacks(rawHeaders.toIterator)

    keys.foreach { key =>
      stacks.contains(key.toLowerCase) should be(true)
    }
    stacks.size should be (keys.size)

    val dkim = stacks.get(DkimSignature.DkimHeader.toLowerCase).get
    dkim.size should be (2)

    val from = stacks.get("test-header").get
    from.size should be (2)
    from.pop.value should include ("attack")
  }

  "getDkim" should "work with multiple dkim headers" in {

    val stacks = DkimVerifier.buildHeaderStacks(rawHeaders.toIterator)

    val dkim = DkimVerifier.getDkim(stacks)
    dkim.isDefined should be (true)
  }

  "verifyHeaders" should "correctly verifyHeaders valid dkim headers" in {
    val dnsHelper = new DnsHelperStub(Array(dnsRecord))

    val lines = rawHeaders.toIterator
    val result = DkimVerifier.verifyHeaders(lines, dnsHelper)
    result.isDefined() should be (true)
  }

  "relaxedHeaderFormat" should "correctly remove whitespace" in {

    val text1 = "a\t\tb\tc\r\n \r\n"

    DkimVerifier.relaxedHeaderFormat(text1) should be ("a b c")
  }

  "relaxedBodyFormat" should "correctly remove whitespace for whole body" in {

    val text = "  C \r\n\r\n   D \tE\t\r\n"
    val expected = " C\r\n\r\n D E\r\n"

    val input = new ByteArrayInputStream(text.getBytes)
    val bytes = new Array[Byte](text.length)

    var length = DkimVerifier.relaxedBodyFormat(input, bytes, text.length)
    length should be (expected.length)
    new String(bytes).substring(0, length.toInt) should be (expected)
  }

  "ignoreExtraEndLines" should "correctly skip duplicate CRLF's at the end" in {

    val text = "  C \r\n\r\n   D \t E \r\n\r\n\r\n"

    var length = DkimVerifier.ignoreExtraEndLines(text.getBytes, text.length)
    length should be (text.length - 4)
  }

}
