package org.emailscript.helpers

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * A map that lets you set a value just once.
 * Any subsequent attempts to set a value results in an exception thrown.
 * This provides a way to allow delayed initialization of values
 */
class ValuesMap {

  import ValuesMap.logger
  private val values = mutable.Map[Symbol, Any]()

  def set(symbol: Symbol, o: Any) = {
    if (values.contains(symbol))
      logger.error(s"ValuesMap, trying to reset: $symbol, to $o current value is ${values.getOrElse(symbol, "nothing")}")
    else
      values += (symbol -> o)
  }

  def getOrElse[T](symbol: Symbol, other: T): T = values.getOrElse(symbol, other).asInstanceOf[T]
}

object ValuesMap {

  val logger = LoggerFactory.getLogger(getClass)
  def apply() = new ValuesMap()
}
