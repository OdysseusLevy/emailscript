package org.emailscript.api

import java.util.Date

import scala.beans.BeanProperty

/**
 * Used to track time an inbox was scanned
 */
class LastScan {
  @BeanProperty var start: Date = new Date()
  @BeanProperty var stop: Date = null
  @BeanProperty var lastId:Long = 0

  override def toString(): String = {s"lastId: ${lastId} start: ${start} stop:${stop}"}
}

object LastScan {
  def apply(start: Date, stop: Date, lastId: Long): LastScan = {
    val bean = new LastScan
    bean.setStart(start)
    bean.setStop(stop)
    bean.setLastId(lastId)
    bean
  }
}
