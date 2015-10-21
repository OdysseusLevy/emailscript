package org.emailscript.helpers

import java.io.{Writer, StringReader, Reader, StringWriter}

class TestStreamHandler(var writers: Map[String,StringWriter]) extends StreamHandler {
  override def getReader(name: String): Option[Reader] = {
    if (writers.contains(name))
      Some(new StringReader(writers(name).getBuffer.toString))
    else
      None
  }

  override def getWriter(name: String): Writer = {
    if (writers.contains(name))
      writers(name)
    else {
      val writer = new StringWriter()
      writers += (name -> writer)
      writer
    }
  }

  override def getAllReaders(): Array[(String, Reader)] = Array()
}
