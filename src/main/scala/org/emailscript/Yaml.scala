package org.emailscript

import java.io._

import org.emailscript.beans.EmailAccountBean
import org.yaml.snakeyaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.{Node, Tag}
import org.yaml.snakeyaml.representer.{Represent, Representer}
import org.yaml.snakeyaml.{DumperOptions, TypeDescription}

/**
 * Responsible for loading and storing yaml based data
 */
class Yaml(val dataDir: File, var dataFiles: Map[String,File]) {

  def getOrElse[T](name: String, callback: () => T): T = {
    val dataName = name + Yaml.ymlExtension

    if(!dataFiles.contains(dataName))
      callback()
    else
      Yaml.readFromFile(dataFiles(dataName))
  }

  def set(name: String, data: AnyRef) {

    val dataName = name + Yaml.ymlExtension
    if(!dataFiles.contains(dataName)) {
      val dataFile = new File(dataDir, dataName)
      if(!dataFile.createNewFile())
        throw new Exception(s"Can not create file ${dataName} in directory: ${dataDir.getName}")

      dataFiles = dataFiles + (dataName -> dataFile)
    }

    Yaml.saveToFile(data, dataFiles(dataName))

  }
}

object filter extends FilenameFilter {
  def accept(dir: File, name: String) = { name.endsWith(Yaml.ymlExtension)}
}

object Yaml {

  val ymlExtension = ".yml"
  val defaultDataDirName = "data"

  def apply(dirName: String = defaultDataDirName) = new Yaml(new File(dirName), getDataFiles(new File(defaultDataDirName)))

  def readFromFile[T](file: File): T = {
    val yaml = createYaml()
    val reader = new FileReader(file)

    try {
      yaml.load(reader).asInstanceOf[T]
    } finally {
      reader.close()
    }

  }

  private def createYaml(): org.yaml.snakeyaml.Yaml = {
    val constructor = new Constructor()
    constructor.addTypeDescription( new TypeDescription(classOf[EmailAccountBean], new Tag("!EmailAccount")) )
    val options = new DumperOptions()
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    new snakeyaml.Yaml(constructor, new Representer(), options)
  }

  def saveToFile(data: AnyRef, file: File) = {

    val yaml = createYaml()
    yaml.dump(data, new FileWriter(file))
  }

  def getDataFiles(dataDir: File): Map[String, File] = {

    if (!dataDir.exists()) {
      if (!dataDir.mkdir())
        throw new Exception(s"Could not create data directory ${dataDir.getPath}")
    }

    dataDir.listFiles(filter).map(file => file.getName -> file).toMap
  }

  val whoTag = new Tag("!Who")

  def main(args: Array[String]) {

    val constructor = new Constructor()
    constructor.addTypeDescription( new TypeDescription(classOf[TestWho], whoTag) )
    val options = new DumperOptions()
    //options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setAllowReadOnlyProperties(true)
    val yaml = new org.yaml.snakeyaml.Yaml(constructor, representer, options)

    val who = new TestWho("personal", "test@test.com")

    val result = yaml.dump(who)
    println(result)
    val whoIn = yaml.load(result)
    println(s"$whoIn")
  }

  class TestWho(val personal: String, val email: String){

    def getPersonal = personal
    def getEmail = email

    override def toString() = s"$personal<$email>"
  }

  val representer = new Representer {

//    val whoRepresent = new Represent {
//      override def representData(data: scala.Any): Node = {
//        val who = data.asInstanceOf[TestWho]
//        val sequence = Seq(who.personal, who.email).asJava
//        representSequence(whoTag, sequence, true )
//      }
//    }

    addClassTag(classOf[EmailAccountBean], new Tag("!Account"))
    //addClassTag(classOf[TestWho], whoTag)
    //representers.put(classOf[TestWho], whoRepresent)
  }
}