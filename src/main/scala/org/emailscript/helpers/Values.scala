package org.emailscript.helpers

import java.util.function.BiFunction

import com.google.common.base.Strings
import org.emailscript.api.Who

import scala.collection.JavaConverters._

/**
 * Lets us add values for objects
 */
object Values {

  lazy val yaml = Yaml()

  private val valuesFile = "ms_values"
  private lazy val valuesMap: ConcurrentMap = load()

  private type ValuesMap = Map[String, AnyRef]
  private type ConcurrentMap = java.util.concurrent.ConcurrentHashMap[AnyRef, ValuesMap ]
  private type JavaValues = java.util.Map[String, AnyRef]
  private type JavaMap = java.util.HashMap[AnyRef, JavaValues]

  def setValue(o: AnyRef, _name: String, _value: AnyRef): Unit = {

    val name = _name.toLowerCase
    valuesMap.putIfAbsent(o, Map())

    val update = new BiFunction[AnyRef,ValuesMap,ValuesMap] {
      override def apply(key: AnyRef, original: ValuesMap): ValuesMap = {
        original + (name -> _value)
      }
    }

    valuesMap.compute(o, update)
  }

  def hasValue(o: AnyRef, _name: String): Boolean = {

    val name = _name.toLowerCase
    if (Strings.isNullOrEmpty(name) || !valuesMap.containsKey(o))
      false
    else{
      valuesMap.get(o).contains(name)
    }
  }

  def removeValue(o: AnyRef, _name: String) = {

    val name = _name.toLowerCase
    if (!Strings.isNullOrEmpty(name) && valuesMap.containsKey(o))
      valuesMap.put(o, valuesMap.get(o) - name)
  }

  def getValue(o: AnyRef, _name: String): AnyRef = {
    val name = _name.toLowerCase
    valuesMap.putIfAbsent(o, Map())
    valuesMap.get(o).getOrElse(name, "")
  }

  def toJava(inMap: ConcurrentMap = valuesMap): JavaMap = {
    val map = new JavaMap()

    for( (who: Who, values: ValuesMap) <- inMap.asScala) {
      val valuesMap = values.asJava
      map.put(who, valuesMap)
    }
    map
  }

  def toConcurrent(inMap: JavaMap): ConcurrentMap = {
    val map = new ConcurrentMap()

    for( (who: Who, values: JavaValues) <- inMap.asScala) {
      val valuesMap = values.asScala.map { case (key: String,value: AnyRef) => (key -> value)}.toMap
      map.put(who, valuesMap)
    }
    map
  }

  def save(): Unit = yaml.set(valuesFile, toJava())

  def load(): ConcurrentMap = {
    val data = yaml.getOrElse(valuesFile, () => new JavaMap())
    toConcurrent(data)
  }
}
