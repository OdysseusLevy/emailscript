package org.emailscript.api

import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date

import com.google.gson.Gson
import org.emailscript.helpers.{Exporter, Importer, LoggerFactory}
import uk.co.bigbeeconsultants.http.HttpClient
import uk.co.bigbeeconsultants.http.header.MediaType
import uk.co.bigbeeconsultants.http.request.RequestBody

import scala.beans.BeanProperty

class NoteBean {
  @BeanProperty var uid_l: Long = 0
  @BeanProperty var from_s: String = ""
  @BeanProperty var subject_s: String = ""
  @BeanProperty var note_t: String = ""
}

object NoteBean {
  def apply(email: Email, note: String) = {
    val bean = new NoteBean

    bean.uid_l = email.getUid()
    bean.from_s = email.getFrom().toString
    bean.subject_s = email.getSubject()
    bean.note_t = note

    bean
  }
}

class IndexEmailBean {

  @BeanProperty var id: String = ""
  @BeanProperty var from_s: String = ""
  @BeanProperty var received_dt: String = ""
  @BeanProperty var body_t: String = ""
  @BeanProperty var subject_s: String = ""
  @BeanProperty var folder_s: String = ""
}

object IndexEmailBean {
  def apply(email: Email) = {

    val bean = new IndexEmailBean

    bean.id = email.getUid().toString
    bean.from_s = email.getFrom().toString
    bean.received_dt = Indexer.formatDate(email.getReceived())
    bean.body_t = email.getBody()
    bean.subject_s = email.getSubject()
    bean.folder_s = email.getFolder()

    bean
  }
}

class LogBean {
  @BeanProperty var date_dt: String = ""
  @BeanProperty var text_t: String = ""
  @BeanProperty var level_s: String = ""
  @BeanProperty var thread_s: String = ""
}

object LogBean {
  def apply (timeStamp: Long, text: String, level: String, thread: String) = {
    val bean = new LogBean

    bean.date_dt = Indexer.formatDate(timeStamp)
    bean.text_t = text
    bean.level_s = level
    bean.thread_s = thread

    bean
  }
}

class IndexerBean() extends NamedBean with Importer {
  @BeanProperty var url: String = ""
  override def doImport(): AnyRef = Indexer(this)
}

/**
 * Provide search and indexing support using the Lucene search engine
 */
class Indexer(val url: String) extends Exporter {

  import org.emailscript.api.Indexer._

  val httpClient = new HttpClient

  override def doExport(): AnyRef = {

    val bean = new IndexerBean()
    bean.url = url
    bean
  }

  //
  // Public API
  //

  def indexNote(email: Email, note:String) = {

    val noteBean = NoteBean(email, note)
    index(noteBean)
  }

  def indexEmail(email: Email) = {
    val emailBean = IndexEmailBean(email)
    index(emailBean)
  }

  /**
   * Add this email to the search index, so that we can find it later
   */
  def index(data: AnyRef): Unit = {

    val gson = new Gson();

    val body = gson.toJson(Array(data))
    val requestBody = RequestBody(body, MediaType.APPLICATION_JSON)

    val response = httpClient.post(new URL(url), Some(requestBody))

    logger.info(s"indexing to url: $url body: $body ")

    if (!response.status.isSuccess)
      logger.info(s"status: ${response.status.message} response = ${response.body.asString}")
  }

}

object Indexer {

  val logger = LoggerFactory.getLogger(getClass)
  val dateFormatter = DateTimeFormatter.ISO_INSTANT
  val CommitParam = "?commitWithin=5000" // tells solr to commit transactions with 5 seconds

  def makeUpdateCommand(url: String) = url + "/update" + CommitParam
  def apply(bean: IndexerBean) = new Indexer(makeUpdateCommand(bean.url))
  def apply(url: String) = new Indexer(makeUpdateCommand(url))

  def formatDate(date: Date) = dateFormatter.format(date.toInstant)
  def formatDate(timeStamp: Long) = dateFormatter.format(Instant.ofEpochMilli(timeStamp))
}
