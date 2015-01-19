package org.emailscript.api
import org.emailscript.helpers.ValuesMap
import scala.collection.concurrent.{TrieMap}

trait ValuesBean {
  def set(symbol: Symbol, value: Any)
  def getOrElse[T](symbol: Symbol, other: T): T
}

/**
 * Mutable in that keys can be added,
 * But immutable in that values for a key can never change
 */
trait ValuesImmutableBean extends ValuesBean {
  private val _values = ValuesMap()

  def set(symbol: Symbol, value: Any) = _values.set(symbol, value)
  def getOrElse[T](symbol: Symbol, other: T): T = _values.getOrElse(symbol, other)
}

trait ValuesMutableBean extends ValuesBean {

  private var _values= TrieMap[Symbol, Any]()

  def set(symbol: Symbol, value: Any) = _values.put(symbol, value)
  def getOrElse[T](symbol: Symbol, other: T): T = _values.getOrElse(symbol, other).asInstanceOf[T]
}

/**
 * Named accounts
 */
trait NamedBean extends ValuesBean {
  /**
   * This is the name that will show up in your script. For example if you set this to "MySpecialName", then in your
   * script you will have a variable named "MySpecialName".
   *
   * This name needs to be unique across all of your accounts (or an error gets thrown)
   * @group Properties
   */
  def setNickname(name: String): Unit = set('Nickname,name)

  /**
   * @group Properties
   */
  def getNickname(): String = getOrElse('Nickname, "")
}
