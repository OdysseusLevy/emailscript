package org.emailscript.mail.dkim

import org.emailscript.helpers.DnsHelper
import org.scalatest.{Matchers, FlatSpec}

class DnsHelperStub(result: Array[String]) extends DnsHelper {
  override def getDnsRecords(domain: String, dnsType: String) = result
}

object DkimDnsLookupTest{

  val invalid =  """v=DKIM2; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuThrbTu+nsO0xUR7Ht1mF7nAuIaSD0b6OMYxIxIxv3gFWwZj7J4M3TnJ0B78ViZHzWDrBKxBUjamToMD5NmNDsgJGE+Jl/gdkMb8bf9DLlJ69YVfr6SDPizSE8M8dOIbykeHXAev98liQK0GTs2+0i3qxhb19kbPOuq8MNDa+WwZVrH79JUUZCV6blAUGpuqZ17pl9G35" "v7iY21odGO5SoWHhUp5n2kvw5XEjnAOXcq6mJUdczoygqlFjgdu5I1uCOgnCTbE2/RuPy6kmWHa9I0QjxPf7pItQvK5r5cInsHw/s4iVFc1/ULX9nKzkKMN5upRNvHjwDWEAJYZpTcV0wIDAQAB""""
  val valid = """ "v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuThrbTu+nsO0xUR7Ht1mF7nAuIaSD0b6OMYxIxIxv3gFWwZj7J4M3TnJ0B78ViZHzWDrBKxBUjamToMD5NmNDsgJGE+Jl/gdkMb8bf9DLlJ69YVfr6SDPizSE8M8dOIbykeHXAev98liQK0GTs2+0i3qxhb19kbPOuq8MNDa+WwZVrH79JUUZCV6blAUGpuqZ17pl9G35" "v7iY21odGO5SoWHhUp5n2kvw5XEjnAOXcq6mJUdczoygqlFjgdu5I1uCOgnCTbE2/RuPy6kmWHa9I0QjxPf7pItQvK5r5cInsHw/s4iVFc1/ULX9nKzkKMN5upRNvHjwDWEAJYZpTcV0wIDAQAB" """

}
class DkimDnsLookupTest extends FlatSpec with Matchers {

  import DkimDnsLookupTest._

  "getPublicKey" should "do basic validation" in {
    val dnsHelper = new DnsHelperStub(Array(invalid))
    val dnsLookup = new DkimDnsLookup(dnsHelper)

    dnsLookup.getPublicKey("-test-").isEmpty should be(true)
  }

  "getPublicKey" should "skip invalid records and find correct ones" in {
    val dnsHelper = new DnsHelperStub(Array(invalid, valid))
    val dnsLookup = new DkimDnsLookup(dnsHelper)

    val result = dnsLookup.getPublicKey("-test-")

    result.isDefined should be(true)
  }


}
