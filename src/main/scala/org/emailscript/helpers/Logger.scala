package org.emailscript.helpers

import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.classic.spi.{StackTraceElementProxy, ILoggingEvent}
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.util.StatusPrinter
import com.google.gdata.util.common.base.StringUtil
import org.emailscript.api.{LogBean, Indexer}
import org.slf4j.{Logger => SlfjLogger, LoggerFactory => SlfjLoggerFactory, MDC}

/**
 * Centralizes logging and hides logging implementation details
 */
class Logger(logger: SlfjLogger) {

  def debug(text: String): Unit = logger.debug(text)
  def debug(text: String, t: Throwable): Unit = logger.debug(text, t)

  def info(text: String): Unit = logger.info(text)
  def info(text: String, t: Throwable): Unit = logger.info(text, t)

  def warn(text: String): Unit = logger.warn(text)
  def warn(text: String, t: Throwable): Unit = logger.warn(text, t)

  def error(text: String): Unit = logger.error(text)
  def error(text: String, t: Throwable): Unit = logger.error(text, t)

}

class IndexAppender extends AppenderBase[ILoggingEvent] {

  var url: String = ""

  var level: Level = Level.WARN

  lazy val indexer: Indexer = Indexer(url)

  def getUrl() = url
  def setUrl(url: String) = this.url = url

  def getLevel() = level
  def setLevel(level: Level) = this.level = level

  override def start(): Unit = {
    if (StringUtil.isEmpty(url))
      throw new Exception("Missing url in the Logging indexer configuration")

    super.start()
  }

  override def append(e: ILoggingEvent): Unit = {
    if (!e.getLevel.isGreaterOrEqual(level))
      return

    var message =  e.getFormattedMessage
    if (e.getThrowableProxy != null) {
      message +=  " "  + e.getThrowableProxy.getMessage + "\n" + getStackTrace(e.getThrowableProxy.getStackTraceElementProxyArray)
    }

    val bean = LogBean(e.getTimeStamp, e.getFormattedMessage, e.getLevel.toString, e.getThreadName)
    indexer.index(bean)
  }

def getStackTrace(elements: Array[StackTraceElementProxy]): String = {
  if (elements == null)
    return ""

  var result = "Stack trace: "
  elements.foreach(element => result += s"\n${element.toString}")

  result
}

}

object Logger {

  /**
   * Use this when something is not working with the logger
   */
  def debugLogging() {
    val lc = SlfjLoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    StatusPrinter.print(lc)
  }

  //
  // Configure our logger to output to <scriptName>.log (see logback.xml config file)
  //
  def setScriptLogName(scriptName: String) = {
    MDC.put("script", scriptName)
  }
}

object LoggerFactory {

  def getLogger(name: String) = SlfjLoggerFactory.getLogger(name)
  def getLogger(c: Class[_]): Logger = new Logger(SlfjLoggerFactory.getLogger(c))
}

