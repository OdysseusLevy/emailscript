package org.emailscript.api
import scala.beans.BeanProperty

trait Named {
  /**
   * This is the name that will show up in your script. For example if you set this to "MySpecialName", then in your
   * script you will have a variable named "MySpecialName".
   *
   * This name needs to be unique across all of your accounts (or an error gets thrown)
   * @group Properties
   */
  def setNickname(name: String): Unit

  def getNickname(): String
}

object Named {
  val Nickname = "Nickname"
}

trait NamedBean extends Named  {
  @BeanProperty var nickname: String = ""
}
