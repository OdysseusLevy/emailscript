package org.emailscript.api

import java.io.StringWriter
import java.text.DecimalFormat
import java.time.{LocalDate}
import javax.script.Bindings

import com.github.mustachejava.DefaultMustacheFactory
import org.emailscript.helpers.DnsHelper
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._

object Helper {
  val logger = LoggerFactory.getLogger(getClass)
}
/**
 * Useful utilities
 *
 * ==Features==
 *  - Use mustache templates @see [[http://mustache.github.io/mustache.5.body]]
 *  - Simple date calculation
 *  - Simple number formatting
 */
class Helper( scope: Bindings ) {

  import Helper.logger

  private val percentFormat = new DecimalFormat("###.##%")

  private val mf = new DefaultMustacheFactory();

  private def checkParam(param: String) = {
    if (!scope.containsKey(param)){
      val error = s"Missing required parameter $param}"
      logger.error(error)
      throw new RuntimeException(error)
    }
  }

  private def requires(params: Iterable[String]) = params.foreach(checkParam(_))

  /**
   * Check to see if the specified parameters are supplied to this script.
   * If not, halts execution and prints an appropriate error message
   *
   * @param params list of parameters that are expected
   */
  def requires(params: java.util.Collection[String]): Unit = requires(params.asScala)

  /**
   * You can do text formatting using mustache templates
   * @see [[http://mustache.github.io/mustache.5.body]]
   *
   * @param templateName filename of mustache template
   * @param data data to feed into template
   * @return formatted result
   */
  def execute(templateName: String, data: AnyRef): String = {
    val mustache = mf.compile(templateName)
    val output = new StringWriter()
    mustache.execute(output, data).flush()
    output.toString
  }

  /**
   * What was the date x days ago?
   * @param days
   * @return java.sql.Date (because the Java 8 date is not yet commonly used)
   */
  def daysAgo(days: Int) = java.sql.Date.valueOf(LocalDate.now().minusDays(days))

  /**
   * What was date x weeks ago?
   * @param weeks
   * @return java.sql.Date (because the Java 8 date is not yet commonly used)
   */
  def weeksAgo(weeks: Int) = java.sql.Date.valueOf(LocalDate.now().minusWeeks(weeks))

  /**
   * What was the date x months ago?
   * @param months
   * @return java.sql.Date ((because the Java 8 date is not yet commonly used)
   */
  def monthsAgo(months: Int) = java.sql.Date.valueOf(LocalDate.now().minusMonths(months))

  /**
   * Takes two numbers and returns what percent the numerator is of the denominator as a formatted string.
   *
   * Ex. toPercent(3,9) == "33.33%"
   *     toPercent(2,4) == "50%"
   *     toPercent(100,50) == "200%"
   *
   */
  def toPercent(numerator: Long, denominator: Long): String = {
    val fraction = numerator.toFloat / denominator.toFloat
    percentFormat.format(fraction)
  }

  def toPercent(number: Double): String = percentFormat.format(number)

  /**
   * Format a number using a format string
   * @see [[http://docs.oracle.com/javase/tutorial/i18n/format/decimalFormat.body]]
   *
   * @param number number to format
   * @param format format to use for example ###,###
   * @return formatted text returned from running the mustache template
   *
   */
  def formatNumber(number: Double, format: String) = {
    val formatter = new DecimalFormat(format)
    formatter.format(number)
  }

  /**
   * Takes a number and returns it formatted in KB,MB,GB etc.
   *  @param number
   */
  def toBytes(number: Long): String = {
    /*
     Uses the fact that Size units have a distance of 10 bits (1024=2^10) meaning the position of the highest 1 bit
      - or in other words the number of leading zeros - differ by 10 (Bytes=KB*1024, KB=MB*1024 etc.).

     From: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    */

    if (number < 1024)
      number + " B"
    else {
      val z = (63 - java.lang.Long.numberOfLeadingZeros(number)) / 10;
      val rounded = number.toDouble / (1L << (z*10))
      "%.1f %sB".format(rounded, " KMGTPE".charAt(z));
    }
  }

  /**
   * Do a dns lookup
   * @param domain domain to query
   * @param dnsType what kinds of dns record is expected (eg. TXT, A, MX) defaults to returning all types
   * @return Array of text results
   */
  def getDnsRecords(domain: String, dnsType: String): Array[String] = {
    new DnsHelper().getDnsRecords(domain, dnsType)
  }
}
