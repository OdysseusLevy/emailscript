package org.emailscript.dkim

import org.scalatest.{FlatSpec, Matchers}

object DkimSignatureTest {

  val fields1=
  """
    |v=1 ; b = abcd=efgh=ijk; bh=   ;f=;
  """.stripMargin

  val badFields = "v=1; a;"

  val bodyhash1 = """iYDR0YLgGmEaV2kWo/y4JNKl5jg="""
  val signature1 = """flrxE6btcfXlUql85qVNmtaf6kGNc8rCWh6f14bsuOF3XgcIwN3ltxMLKNucreMBt9Cj/jYARFNkFF5g06oFZDv2UvQbRraNdLLHySPWlb+f1sjSFHRN0lxEH4uFXnl78JgAloKCcZkctdqupuQSN79nubNV8VrHT4CWFwPA1Uk="""

  val header1 =
    """v=1; a=rsa-sha1; c=relaxed/simple; s=southwest; d=luv.southwest.com;
      |  h=MIME-Version:Content-Type:Content-Transfer-Encoding:Date:To:From:Reply-To:Subject:List-Unsubscribe:Message-ID; i=RapidRewards@luv.southwest.com;
      |  bh=iYDR0YLgGmEaV2kWo/y4JNKl5jg=;
      |  b=flrxE6btcfXlUql85qVNmtaf6kGNc8rCWh6f14bsuOF3XgcIwN3ltxMLKNucreMBt9Cj/jYARFNk
      |  FF5g06oFZDv2UvQbRraNdLLHySPWlb+f1sjSFHRN0lxEH4uFXnl78JgAloKCcZkctdqupuQSN79n
      |  ubNV8VrHT4CWFwPA1Uk=""".stripMargin

  val headerWithoutHTag =
    """ v=1; a=rsa-sha256; d=example.net; s=brisbane;
      |     c=relaxed/simple; q=dns/txt; l=1234; t=1117574938; x=1118006938;
      |     bh=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=;
      |     b=dzdVyOfAKCdLXdJOc9G2q8LoXSlEniSbav+yuU4zGeeruD00lszZ
      |              VoG4ZHRNiYzR
    """.stripMargin

}

class DkimSignatureTest extends FlatSpec with Matchers {

  import DkimSignatureTest._

  "mapFields" should "correctly split up header fields" in {
    val map = DkimSignature.mapFields(fields1)

    map.get("v").get should be ("1")
    map.get("b").get should be ("abcd=efgh=ijk")
    map.get("bh").get should be ("")
    map.get("f").get should be ("")

  }

  "mapFields" should "reject improperly formatted fields" in {

    var exceptionThrown = false

    try{
      val map = DkimSignature.mapFields(badFields)
    } catch {
      case _: Exception => exceptionThrown = true
    }

    exceptionThrown should be (true)
  }

  "create" should "parse correct signatures" in {

    val result = DkimSignature.create(header1)

    result.isDefined should be (true)
    val signature = result.get

    signature.headerCanonicalization should be ("relaxed")
    signature.bodyCanonicalization should be ("simple")
    signature.bodyHash should be (bodyhash1)
    signature.signature should be (signature1)
  }

  "create" should "return None with missing mandatory fields" in {

    val signature = DkimSignature.create(headerWithoutHTag)
    signature.isDefined should be (false)
  }
}
