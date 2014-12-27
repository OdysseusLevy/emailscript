package org.emailscript.beans

import java.util.Date

import scala.beans.BeanProperty

/**
 * Used to track time an inbox was scanned
 */
class LastScan {
  @BeanProperty var start: Date = new Date() //TODO switch to Java 8 dates
  @BeanProperty var stop: Date = null
  @BeanProperty var lastId:Long = 0

  override def toString(): String = {s"lastId: ${lastId} start: ${start} stop:${stop}"}
}
