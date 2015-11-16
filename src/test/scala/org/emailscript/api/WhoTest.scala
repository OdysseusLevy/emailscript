package org.emailscript.api

import org.scalatest.{Matchers, FlatSpec}

class WhoTest extends FlatSpec with Matchers {

  val who1 = Who("Test1","ABC@Test.com")
  val who2 = Who("Test2", "abc@tEst.com")

  "Who" should "just use case insensitive email for equality checks" in {

    who1 should be(who2)
  }

  "Who" should "hash using just case insensitive email" in {

    who1.hashCode() should be (who2.hashCode())
  }

}
