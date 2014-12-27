package org.emailscript

import java.io.StringWriter
import java.text.{DecimalFormat}

import com.github.mustachejava.DefaultMustacheFactory

/**
 * handles templating
 */
object Template {

  val percentFormat = new DecimalFormat("###.##%")

  val mf = new DefaultMustacheFactory();

  def execute(templateName: String, data: AnyRef): String = {
    val mustache = mf.compile(templateName)
    val output = new StringWriter()
    mustache.execute(output, data).flush()
    output.toString
  }

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

  def formatNumber(number: Double, format: String) = {
    val formatter = new DecimalFormat(format)
    formatter.format(number)
  }

  /**
   * Takes a number and returns it formatted in KB,MB,GB etc.
   *
   * Uses the fact that Size units have a distance of 10 bits (1024=2^10) meaning the position of the highest 1 bit
   * - or in other words the number of leading zeros - differ by 10 (Bytes=KB*1024, KB=MB*1024 etc.).
   *
   * From: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
   * @param number
   * @return
   */
  def toBytes(number: Long): String = {

    if (number < 1024)
      number + " B"
    else {
      val z = (63 - java.lang.Long.numberOfLeadingZeros(number)) / 10;
      val rounded = number.toDouble / (1L << (z*10))
      "%.1f %sB".format(rounded, " KMGTPE".charAt(z));
    }
  }
}
