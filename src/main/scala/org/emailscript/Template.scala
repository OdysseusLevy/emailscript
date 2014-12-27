package org.emailscript

import java.io.StringWriter

import com.github.mustachejava.DefaultMustacheFactory

/**
 * handles templating
 */
object Template {

  val mf = new DefaultMustacheFactory();

  def execute(templateName: String, data: AnyRef): String = {
    val mustache = mf.compile(templateName)
    val output = new StringWriter()
    mustache.execute(output, data).flush()
    output.toString
  }
}
