package org.emailscript.helpers

import java.util.function.BiFunction

import com.google.common.base.Strings

import scala.collection.JavaConverters._

/**
 * Used to attach a value (typically a string) to an object (typically as Who)
 */
class Values(dataName: String, dataHandler: DataHandler) {

  private type ValuesMap = Map[String, AnyRef]
  private type ConcurrentMap = java.util.concurrent.ConcurrentHashMap[AnyRef, ValuesMap ]
  private type JavaValues = java.util.Map[String, AnyRef]
  private type JavaMap = java.util.HashMap[AnyRef, JavaValues]

  private lazy val valuesMap: ConcurrentMap = load()

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

    for( (o: AnyRef, values: ValuesMap) <- inMap.asScala) {
      val valuesMap = values.asJava
      map.put(Serializer.convertForExport(o), valuesMap)
    }
    map
  }

  def toConcurrent(inMap: JavaMap): ConcurrentMap = {
    val map = new ConcurrentMap()

    for( (o: AnyRef, values: JavaValues) <- inMap.asScala) {
      val valuesMap = values.asScala.map {
        case (key: String, value: Any) => (key -> value)
        case (key:String, _) => (key -> "")}.toMap
      map.put(Serializer.convertForImport(o), valuesMap)
    }
    map
  }

  def save(): Unit = dataHandler.set(dataName, toJava())

  def load(): ConcurrentMap = {
    val data = dataHandler.getOrElse(dataName, () => new JavaMap())
    toConcurrent(data)
  }

}

object Values {

  var valueCollections = Map[String, Values]()

  def getValues[T <:AnyRef](name: String, yaml: Yaml = Yaml(Configuration.DataDir)): Values = {

    if (valueCollections.contains(name))
      valueCollections(name)
    else {
      val values = new Values(name, yaml)
      valueCollections += (name -> values)
      values
    }
  }

  def save(): Unit = {
    valueCollections.values.foreach{ values =>
      values.save()
    }
  }

}
