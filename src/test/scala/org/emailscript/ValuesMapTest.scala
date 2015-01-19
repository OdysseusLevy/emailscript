package org.emailscript

import org.emailscript.helpers.ValuesMap
import org.scalatest.{FlatSpec, Matchers}

class ValuesMapTest  extends FlatSpec with Matchers{

  "Values Map" should "Allow a value to be set only once" in {

    val map = ValuesMap()

    map.set('test1, 123)
    map.getOrElse('test1, 0) should be (123)

    map.set('test1, 222)
    map.getOrElse('test1, 0) should be (123)
  }

  "Values Map" should "Return correct default values when value is not present" in {

    val map = ValuesMap()

    map.getOrElse('test1, "test1") should be ("test1")
  }
}
