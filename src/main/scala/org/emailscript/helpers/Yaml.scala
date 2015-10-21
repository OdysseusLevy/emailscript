package org.emailscript.helpers

import java.io._

import org.emailscript.api._
import org.yaml.snakeyaml
import org.yaml.snakeyaml.constructor.{Construct, Constructor}
import org.yaml.snakeyaml.nodes.{Node, ScalarNode, Tag}
import org.yaml.snakeyaml.representer.{Represent, Representer}
import org.yaml.snakeyaml.{DumperOptions, TypeDescription}

import scala.collection.JavaConverters._

/**
 * Responsible for loading and storing yaml based data
 */
class Yaml(streamHandler: StreamHandler) extends DataHandler {

  import Yaml.logger

  val yaml = Yaml.createYaml()

  def read(reader: Reader): Option[AnyRef] = {
    try {
      Some(yaml.load(reader))
    } catch {
      case e: Throwable => logger.error(s"Failed to read data", e)
        throw e
    } finally {
      reader.close()
    }
  }

  def getOrElse[T](name: String, callback: () => T): T = {
    val reader = streamHandler.getReader(name)

    if(!reader.isDefined)
      return callback()

    read(reader.get).getOrElse(callback()).asInstanceOf[T]
  }

  def set(name: String, data: AnyRef) {
    save(data,streamHandler.getWriter(name) )
  }

  def save(data: AnyRef, writer: Writer) = {

    val yaml = Yaml.createYaml()

    try {
      yaml.dump(data, writer)
    } catch {
      case e: Throwable => logger.error(s"Failed to save yaml data", e);
        throw e
    } finally{
      writer.close()
    }
  }
}

case class YamlType(tag: Tag, clazz: Class[AnyRef], converter: Option[AnyRef=>AnyRef])
object YamlType {
  def apply(tagName: String, clazz: Class[_], converter: Option[AnyRef=>AnyRef] = None) = {
    new YamlType(new Tag(tagName), clazz.asInstanceOf[Class[AnyRef]], converter)
  }
}

object Yaml {

  val logger = LoggerFactory.getLogger(getClass)

  val WhoTag ="!Who"
  val EmailAccountTag = "!EmailAccount"
  val GoogleContactsTag = "!GoogleContacts"
  val LastScanTag = "!LastScan"
  val IndexerTag = "!Indexer"

  val customTagTypes = Array[YamlType](
    YamlType(WhoTag, classOf[WhoBean]),
    YamlType(EmailAccountTag, classOf[EmailAccountBean]),
    YamlType(GoogleContactsTag, classOf[GoogleContactsBean]),
    YamlType(LastScanTag, classOf[LastScan]),
    YamlType(IndexerTag, classOf[IndexerBean])
  )

  val ymlExtension = ".yml"

  def apply(handler: StreamHandler) = new Yaml(handler)

  def apply(dirName: String) = {
    val handler = new FileHandler(new File(dirName), ymlExtension)
    new Yaml(handler)
  }

  private def createYaml(): org.yaml.snakeyaml.Yaml = {
    val constructor = new Constructor()
    val representer = new Representer()

    customTagTypes.foreach { case yamlType: YamlType =>
      constructor.addTypeDescription( new TypeDescription(yamlType.clazz, yamlType.tag))
      representer.addClassTag(yamlType.clazz, yamlType.tag)
    }

    val options = new DumperOptions()
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    new snakeyaml.Yaml(constructor, representer, options)
  }

  /**
   *  ** Experimental code **
   *
   *  exploring creating an object without using a javabean as intermediary
   */

  def main(args: Array[String]) {

    constructor.addTypeDescription( new TypeDescription(classOf[TestWho], WhoTag) )
    val options = new DumperOptions()
    options.setAllowReadOnlyProperties(true)

    val representer = new Representer()
    representer.addClassTag(classOf[TestWho], new Tag(WhoTag))
    val yaml = new snakeyaml.Yaml(constructor, representer, options)

    val who = new TestWho("personal", "test@test.com")

    val result = yaml.dump(who)
    println(result)
    val whoIn = yaml.load(result)
    println(s"$whoIn")
  }

  val constructor = new Constructor {
    this.yamlConstructors.put(new Tag(WhoTag), new WhoConstruct)
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
        representSequence(new Tag(WhoTag), sequence, true)
      }
    }
  }

}