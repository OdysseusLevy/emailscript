package org.emailscript.dnsbl

import org.scalatest.FlatSpec

class SurblDnsblTest extends FlatSpec {

  "Surbl.getDomains" should "separate out domains correctly" in {

    assertResult(Subdomains(), "no domains")(SurblDnsbl.getSubdomains("abc"))
    assertResult(Subdomains("abc.com"), "2 domains")(SurblDnsbl.getSubdomains("abc.com"))
    assertResult(Subdomains("abc.com", "def.abc.com"), "3 domains")(SurblDnsbl.getSubdomains("def.abc.com"))
    assertResult(Subdomains("abc.com", "def.abc.com", "ghi.def.abc.com"), "4 domains")(SurblDnsbl.getSubdomains("ghi.def.abc.com"))
    assertResult(Subdomains("abc.com", "def.abc.com", "ghi.def.abc.com"), "> 4 domains")(SurblDnsbl.getSubdomains("xxx.ghi.def.abc.com"))
  }

  "Surbl.getName" should "use tld sets to guide which name to use" in {

    val surbl: SurblDnsbl = new SurblDnsbl(Set("com.au"), Set("wa.edu.au"))

    assertResult("abc.com.au", "found 2 domain")(surbl.getName("def.abc.com.au"))
    assertResult("abc.wa.edu.au", "found 3 domains")(surbl.getName("def.abc.wa.edu.au"))
    assertResult("abc.com", "default to 2 domains")(surbl.getName("def.abc.com"))
  }
}