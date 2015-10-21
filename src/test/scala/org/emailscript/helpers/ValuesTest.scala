package org.emailscript.helpers

import org.emailscript.api.Who
import org.emailscript.api.Who
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.{Matchers, FlatSpec}

class ValuesTest extends FlatSpec with Matchers {

  def doValuesTest(values: Values, who1: Who, who2: Who):Unit = {
    values.getValue(who1, "value1") should be ("data1")
    values.getValue(who1, "value2") should be ("data2")
    values.getValue(who1, "value3") should be ("")

    values.getValue(who2, "value1") should be ("data3")
    values.getValue(who2, "value2") should be ("")
  }

  "Who class" should "work correctly with setValue" in {

    val handler = new SimpleDataHandler
    val values1 = new Values("test", handler)
    val who1 = Who("who1","who1@test.org")
    val who2 = Who("who2","who2@test.org")

    values1.setValue(who1, "value1", "data0")
    values1.setValue(who1, "value2", "data2")
    values1.setValue(who1, "value1", "data1")
    values1.setValue(who2, "value1", "data3")

    doValuesTest(values1, who1, who2)

    values1.save()

    val values2 = new Values("test", handler)
    doValuesTest(values2, who1, who2)
  }

}

