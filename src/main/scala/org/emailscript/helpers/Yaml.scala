package org.emailscript.helpers

import java.io._

import org.emailscript.api.{GoogleContacts, LastScan, Who, EmailAccount}
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml
import org.yaml.snakeyaml.constructor.{Construct, Constructor}
import org.yaml.snakeyaml.nodes.{Node, ScalarNode, Tag}
import org.yaml.snakeyaml.representer.{Represent, Representer}
import org.yaml.snakeyaml.{DumperOptions, TypeDescription}

import scala.collection.JavaConverters._

/**
 * Responsible for loading and storing yaml based data
 */
class Yaml(val dataDir: File, var dataFiles: Map[String,File]) {

  def getOrElse[T](name: String, callback: () => T): T = {
    val dataName = name + Yaml.ymlExtension

    if(!dataFiles.contains(dataName))
      return callback()

    Yaml.readFromFile(dataFiles(dataName)).getOrElse(callback()).asInstanceOf[T]
  }

  def set(name: String, data: AnyRef) {

    val dataName = name + Yaml.ymlExtension
    if(!dataFiles.contains(dataName)) {
      val dataFile = new File(dataDir, dataName)
      if(!dataFile.createNewFile())
        throw new Exception(s"Can not create file $dataName in directory: ${dataDir.getName}")

      dataFiles = dataFiles + (dataName -> dataFile)
    }

    Yaml.saveToFile(data, dataFiles(dataName))
  }
}

object filter extends FilenameFilter {
  def accept(dir: File, name: String) = { name.endsWith(Yaml.ymlExtension)}
}

object Yaml {

  val logger = LoggerFactory.getLogger(getClass)

  val WhoTag = new Tag("!Who")
  val EmailAccountTag = new Tag("!EmailAccount")
  val GoogleContactsTag = new Tag("!GoogleContacts")
  val LastScanTag = new Tag("!LastScan")

  val tagMap = Map(
    WhoTag -> classOf[Who],
    EmailAccountTag -> classOf[EmailAccount],
    GoogleContactsTag -> classOf[GoogleContacts],
    LastScanTag -> classOf[LastScan]
  )

  val yaml = createYaml()

  val ymlExtension = ".yml"
  val defaultDataDirName = "data"

  def apply(dirName: String = defaultDataDirName) = new Yaml(new File(dirName), getDataFiles(new File(defaultDataDirName)))

  def read(reader: Reader): Option[AnyRef] = {
    Option(yaml.load(reader))
  }

  def readFromFile(file: File): Option[AnyRef] = {
    val reader = new FileReader(file)

    try {
      read(reader)
    } catch {
      case e: Throwable => logger.error(s"Failed to read data from file: ${file.getName}", e)
                           throw e
    } finally {
      reader.close()
    }
  }

  private def createYaml(): org.yaml.snakeyaml.Yaml = {
    val constructor = new Constructor()
    val representer = new Representer()

    tagMap.foreach { case(tag: Tag, c: Class[_]) =>
      constructor.addTypeDescription( new TypeDescription(c, tag))
      representer.addClassTag(c, tag)
    }

    val options = new DumperOptions()
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    new snakeyaml.Yaml(constructor, representer, options)
  }

  def save(data: AnyRef, writer: Writer) = {
    val yaml = createYaml()
    yaml.dump(data, writer)
  }

  def saveToFile(data: AnyRef, file: File) = {

    val writer = new FileWriter(file)
    try {
      save(data, writer)
    } catch {
      case e: Throwable => logger.error(s"Failed to save yaml data to file ${file.getName}", e);
                           throw e
    } finally{
      writer.close()
    }
  }

  def getDataFiles(dataDir: File): Map[String, File] = {

    if (!dataDir.exists()) {
      if (!dataDir.mkdir())
        throw new Exception(s"Could not create data directory ${dataDir.getPath}")
    }

    dataDir.listFiles(filter).map(file => file.getName -> file).toMap
  }

  def main(args: Array[String]) {

    val constructor = new Constructor()
    constructor.addTypeDescription( new TypeDescription(classOf[TestWho], WhoTag) )
    val options = new DumperOptions()
    //options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setAllowReadOnlyProperties(true)

    val representer = new Representer()
    val yaml = new snakeyaml.Yaml(constructor, representer, options)

    val who = new TestWho("personal", "test@test.com")

    val result = yaml.dump(who)
    println(result)
    val whoIn = yaml.load(result)
    println(s"$whoIn")
  }

  val constructor = new Constructor {
    this.yamlConstructors.put(WhoTag, new WhoConstruct)
    class WhoConstruct() extends Construct {
      override def construct(node: Node): AnyRef = {
        val whoText = constructScalar(node.asInstanceOf[ScalarNode])

        new TestWho("aa", "bbb")
      }

      override def construct2ndStep(node: Node, `object`: scala.Any): Unit = {}
    }
  }

  class TestWho(val personal: String, val email: String){

    def getPersonal = personal
    def getEmail = email

    override def toString() = s"$personal<$email>"
  }

  val representer = new Representer {

    val whoRepresent = new Represent {
      override def representData(data: scala.Any): Node = {
        val who = data.asInstanceOf[TestWho]
        val sequence = Seq(who.personal, who.email).asJava
        representSequence(WhoTag, sequence, true)
      }
    }
  }

}