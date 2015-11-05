package org.emailscript.api

import org.emailscript.dkim.{DkimResult, DkimSignature}

import scala.beans.BeanProperty

/**
  * Represents results of a DKIM validation
  */
class DkimInfo {

  @BeanProperty var description: String = ""
  @BeanProperty var result: DkimSignature = DkimSignature.empty
  @BeanProperty var signature: String = ""
  @BeanProperty var isValid: Boolean = false

}

object DkimInfo{

  def apply(result: DkimResult) = {
    var info = new DkimInfo()

    info.description = result.description
    info.result = result.dkim
    info.signature = result.dkim.rawSignature
    info.isValid = result.isDefined()

    info
  }
}
