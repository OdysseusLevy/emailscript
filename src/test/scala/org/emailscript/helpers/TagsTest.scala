package org.emailscript.helpers

import org.emailscript.api.{WhoBean, Who}
import org.scalatest.{Matchers, FlatSpec}

class SimpleDataHandler extends DataHandler {

  var map = Map[String, AnyRef]()

  override def set(name: String, obj: AnyRef): Unit = map += (name -> obj)

  override def getOrElse[T](name: String, callback: () => T): T = {
    if (map.contains(name))
      map(name).asInstanceOf[T]
    else
      callback()
  }
}

class TagsTest extends FlatSpec with Matchers {

  def doTagTest(tags: Tags, who1: Who, who2: Who):Unit = {
    tags.hasTag(who1, "tag1") should be (true)
    tags.hasTag(who1, "tag2") should be (true)
    tags.hasTag(who1, "tag3") should be (false)

    tags.hasTag(who2, "tag3") should be (true)
    tags.hasTag(who2, "tag1") should be (false)
  }

  "Who class" should "work correctly with setTag" in {

    val handler = new SimpleDataHandler
    val ignore = new Values("test", handler)

    val tags1 = new Tags("test", handler)
    val who1 = Who("who1","who1@test.org")
    val who2 = Who("who2","who2@test.org")

    tags1.setTag(who1, "tag1")
    tags1.setTag(who1, "tag2")
    tags1.setTag(who2, "tag3")

    doTagTest(tags1, who1, who2)

    tags1.save()

    val tags2 = new Tags("test", handler)
    doTagTest(tags2, who1, who2)
  }

  "NoOne object" should "ignore tags and values" in {

    Who.NoOne.addTag("test")
    Who.NoOne.hasTag("test") should be (false)

    Who.NoOne.setValue("test", "value")
    Who.NoOne.getValue("test") should be ("")
  }


}
