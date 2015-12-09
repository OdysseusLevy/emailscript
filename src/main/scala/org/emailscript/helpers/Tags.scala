package org.emailscript.helpers

import java.util
import java.util.function.BiFunction

import com.google.common.base.Strings
import scala.collection.JavaConverters._

trait TagsI {
  def setTag(o: AnyRef, name: String): Unit
  def hasTag(o: AnyRef, _name: String): Boolean
  def removeTag(o: AnyRef, _name: String): Unit
  def getTags(o: AnyRef): Set[String]
}


class Tags(dataName: String, dataHandler: DataHandler) extends TagsI {

  type TagSet = Set[String]
  type TagMap = java.util.concurrent.ConcurrentHashMap[AnyRef, TagSet ]
  type JavaSet = java.util.Collection[String]  // snakeyaml had weird behavior with Set type
  type JavaMap = java.util.HashMap[AnyRef, JavaSet]

  lazy val tagMap: TagMap = load()


  override def setTag(o: AnyRef, _name: String): Unit = {
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

  override def hasTag(o: AnyRef, _name: String): Boolean = {
    val tag = _name.toLowerCase
    if (Strings.isNullOrEmpty(tag) || !tagMap.containsKey(o))
      false
    else{
      tagMap.get(o).contains(tag)
    }
  }

  override def removeTag(o: AnyRef, _name: String): Unit = {
    val tag = _name.toLowerCase
    if (!Strings.isNullOrEmpty(tag) && tagMap.containsKey(o))
      tagMap.put(o, tagMap.get(o) - tag)
  }

  override def getTags(o: AnyRef): Set[String] = {
    tagMap.putIfAbsent(o, Set())
    tagMap.get(o)
  }

  //
  // Plumbing
  //

  def toJava(inMap: TagMap = tagMap): JavaMap = {
    var map = new JavaMap()

    inMap.asScala.foreach{ case (key:AnyRef, tags:TagSet) =>
      val list = new util.ArrayList[String](tags.asJava)
      map.put(Serializer.convertForExport(key), list)
    }

    map
  }

  def toConcurrent(inMap: JavaMap): TagMap = {
    val map = new TagMap()

    if (inMap == null)
      return map

    inMap.asScala.foreach{ case (key:AnyRef, tags: JavaSet) =>
      val set = Set[String](tags.asScala.toSeq:_*)
      map.put(Serializer.convertForImport(key), set)
    }
    map
  }

  def save(): Unit = dataHandler.set(dataName, toJava())

  def load(): TagMap = {
    val data = dataHandler.getOrElse(dataName, () => new JavaMap())
    toConcurrent(data)
  }
}

object EmptyTags extends TagsI{
  override def setTag(o: AnyRef, name: String): Unit = {}
  override def removeTag(o: AnyRef, _name: String): Unit = {}
  override def getTags(o: AnyRef): Set[String] = Set()
  override def hasTag(o: AnyRef, _name: String): Boolean = false
}

/**
 * Lets us add tags for objects
 */
object Tags {

  var tagCollections = Map[String, Tags]()

  def getTags(name: String, yaml: Yaml = Yaml(Configuration.DataDir)): Tags = {

    if (tagCollections.contains(name))
      tagCollections(name)
    else {
      val tags = new Tags(name, yaml)
      tagCollections += (name -> tags)
      tags
    }
  }

  def save(): Unit = {
    tagCollections.values.foreach{ tags =>
      tags.save()
    }
  }

}
