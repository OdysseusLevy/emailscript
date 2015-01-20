package org.emailscript.helpers

import java.util
import java.util.function.BiFunction

import com.google.common.base.Strings
import org.emailscript.api.Who

import scala.collection.JavaConverters._

/**
 * Lets us add tags for objects
 */
object Tags {

  val tagFile = "ms_tags"

  type TagSet = Set[String]
  type TagMap = java.util.concurrent.ConcurrentHashMap[AnyRef, TagSet ]

  type JavaSet = java.util.Collection[String]
  type JavaMap = java.util.HashMap[AnyRef, JavaSet]

  lazy val yaml = Yaml()
  lazy val tagMap: TagMap = load()

  def setTag(o: AnyRef, _name: String): Unit = {

    if (hasTag(o, _name))
      return // already set

    val tag = _name.toLowerCase
    tagMap.putIfAbsent(o, Set())

    val update = new BiFunction[AnyRef,TagSet,TagSet] {
      override def apply(key: AnyRef, original: TagSet): TagSet = {
        original + tag
      }
    }

    tagMap.compute(o, update)
  }

  def hasTag(o: AnyRef, _name: String): Boolean = {

    val tag = _name.toLowerCase
    if (Strings.isNullOrEmpty(tag) || !tagMap.containsKey(o))
      false
    else{
      tagMap.get(o).contains(tag)
    }
  }

  def removeTag(o: AnyRef, _name: String) = {

    val tag = _name.toLowerCase
    if (!Strings.isNullOrEmpty(tag) && tagMap.containsKey(o))
      tagMap.put(o, tagMap.get(o) - tag)
  }

  def getTags(o: AnyRef): Set[String] = {
    tagMap.putIfAbsent(o, Set())
    tagMap.get(o)
  }

  def getKeys(): scala.collection.mutable.Set[AnyRef] = {
    tagMap.keySet().asScala
  }

  def toJava(inMap: TagMap = tagMap): JavaMap = {
    var map = new JavaMap()

    for( (who: Who, set: Set[String]) <- inMap.asScala) {
      val set = new util.ArrayList[String](inMap.get(who).asJava)
      map.put(who, set)
    }
    map
  }

  def toConcurrent(inMap: JavaMap): TagMap = {
    val map = new TagMap()

    for( (who: Who, tags: JavaSet) <- inMap.asScala) {
      val set = Set[String](tags.asScala.toSeq:_*)
      map.put(who, set)
    }
    map
  }

  def save(): Unit = yaml.set(tagFile, toJava())

  def load(): TagMap = {
    val data = yaml.getOrElse(tagFile, () => new JavaMap())
    toConcurrent(data)
  }
}
