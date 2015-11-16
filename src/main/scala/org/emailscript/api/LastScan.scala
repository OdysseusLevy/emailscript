package org.emailscript.api

import java.util.Date

import scala.beans.BeanProperty

/**
 * Used to track time an inbox was scanned
 * For maximum portability we leave the dates in the old-school Date format
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
