package org.emailscript.helpers

import java.io._

import com.google.common.io.Files

/**
 * Used to handle reading or writing of data.
 * Currently this is just to and from file, but I don't want to make it easy to switch this
 */
trait DataHandler{
  def set(name: String, obj: AnyRef)
  def getOrElse[T](name: String, callback: () => T): T
}

trait StreamHandler {

  def getReader(name: String): Option[Reader]
  def getAllReaders(): Array[(String,Reader)]
  def getWriter(name: String): Writer
}

object filter extends FilenameFilter {
  def accept(dir: File, name: String) = { name.endsWith(Yaml.ymlExtension)}
}

object FileHandler {

  def apply(dataDir: String, extension: String) = new FileHandler(new File(dataDir), extension)
  val defaultDataDirName = "data"
}

class FileHandler(dataDir: File, extension: String) extends StreamHandler {

  def getDataFiles(): Array[File] = {

    if (!dataDir.exists()) {
      if (!dataDir.mkdir())
        throw new Exception(s"Could not create data directory ${dataDir.getPath}")
    }

    dataDir.listFiles(filter)
  }

  def findFile(name: String): Option[File] = {
    getDataFiles().find(_.getName == name)
  }

  override def getAllReaders(): Array[(String,Reader)]= {
    getDataFiles().map{ file: File =>
      val name = Files.getNameWithoutExtension(file.getName)
      (name , new FileReader(file))}
  }

  override def getReader(name: String): Option[Reader] = {
    val dataName = name + extension

    findFile(dataName).map{ new FileReader(_)}
  }

  override def getWriter(name: String): Writer = {
    val dataName = name + extension

    var file = findFile(dataName)
    if(!file.isDefined) {
      val dataFile = new File(dataDir, dataName)
      if(!dataFile.createNewFile())
        throw new Exception(s"Can not create file $dataName in directory: ${dataDir.getName}")
      new FileWriter(dataFile)
    }
    else{
      new FileWriter(file.get)
    }
  }
}

