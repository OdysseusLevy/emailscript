package org.emailscript.api

import java.io.File

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document._
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig, Term}
import org.apache.lucene.queryparser.classic.{QueryParser, MultiFieldQueryParser}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util._
import org.slf4j.LoggerFactory

/**
 * Provide search and indexing support using the Lucene search engine
 */
class Search(homeDir: File) {

  import org.emailscript.api.Search._

  private val index = getIndexDir()
  private val analyzer = new StandardAnalyzer()


  private def getIndexDir() = {
  //TODO centralize directory creation logic like this in a File helper

    if (!homeDir.exists)
      throw new RuntimeException(s"Data directory not found: ${homeDir.toString}")

    val dir = new File(homeDir, IndexDir)
    if (!dir.exists())
      dir.mkdir()

    FSDirectory.open(dir)
  }

  private def createEmailBean(doc: Document): EmailBean = {
    val email = new EmailBean()

    email.setFrom(doc.get(FromEmailName), doc.get(FromEmail))
    email.setSubject(doc.get(Subject))
    email.setBody(doc.get(Body))
    email.setUid(doc.get(UID).toLong)
    email.setFolder(doc.get(Folder))

    email
  }

  //
  // Public API
  //

  /**
   * Add this email to the search index, so that we can find it later
   */
  def index(email: MailMessage): Unit = {
    val iwc = new IndexWriterConfig(Version.LATEST, analyzer)
    val writer = new IndexWriter(index, iwc)

    try {
      val doc = new Document

      doc.add(new StringField(UID, email.getUid().toString, Field.Store.YES))
      doc.add(new StringField(FromEmail, email.getFrom().getEmail, Field.Store.YES))
      doc.add(new TextField(FromEmailName, email.getFrom().getName(), Field.Store.YES))
      doc.add(new LongField(Received, email.getReceived().getTime, Field.Store.YES))
      doc.add(new TextField(Body, email.getBody, Field.Store.YES))
      doc.add(new TextField(Subject, email.getSubject, Field.Store.YES))
      doc.add(new TextField(Folder, email.getFolder, Field.Store.YES))
      writer.updateDocument(new Term("uid", email.getUid().toString), doc)
    } catch {
      case e: Throwable => logger.error("Error creating search document", e)
    }
    writer.close
  }


  def searchAdvanced(query: String, maxHits: Int = DefaultMaxHits): Array[EmailBean] = {

    val reader = DirectoryReader.open(index)
    val searcher = new IndexSearcher(reader)
    val parser = new QueryParser(Subject, analyzer);

    val parsedQuery = parser.parse(query)
    val results = searcher.search(parsedQuery, maxHits)
    results.scoreDocs.map{ sd => searcher.doc(sd.doc)}.map{ createEmailBean(_)}
  }

  /**
   * Given a query, find all matching emails
   *
   * @param query Lucene style query string
   * @param maxHits maximum number of emails to find
   * @return
   */
  def search(query: String, maxHits: Int = DefaultMaxHits): Array[EmailBean] = {

    val reader = DirectoryReader.open(index)
    val searcher = new IndexSearcher(reader)
    val parser = new MultiFieldQueryParser(SearchFields, analyzer);

    val parsedQuery = parser.parse(query)
    val results = searcher.search(parsedQuery, maxHits)
    results.scoreDocs.map{ sd => searcher.doc(sd.doc)}.map{ createEmailBean(_)}
  }

  def search(query: String): Array[EmailBean] = search(query, DefaultMaxHits)

}

object Search {

  val UID = "uid"
  val FromEmail = "from"
  val FromEmailName = "fromName"
  val Subject = "subject"
  val Body = "body"
  val Received = "received"
  val Folder = "folder"

  val IndexDir = "index"

  val SearchFields = Array(FromEmailName, FromEmail, Subject, Body)
  val logger = LoggerFactory.getLogger(getClass)
  val DefaultMaxHits = 50
}
