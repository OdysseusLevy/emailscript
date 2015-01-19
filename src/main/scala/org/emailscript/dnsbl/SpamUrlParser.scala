package org.emailscript.dnsbl

import java.net.URL

import org.apache.commons.lang3.StringEscapeUtils
import org.slf4j.LoggerFactory

object SpamUrlParser {

  val LinkNumLimit = 3 // maximum number of links we will try to parse

  /*
    @Performance

    As of 3/24/2014 the SpamHaus dns white list (http://www.spamhauswhitelist.com/en/techfaq.php)
    does not appear to work. When it is working this is exactly what we want.
    We could check for hosts that are know to be good and know that we can skip them.
   */
  val whitelist = Set("www.w3.org", "www.amazon.com", "www.google.com", "e.quicken.intuit.com") //stub
  val extensionWhitelist = Set("png", "jpg", "jpeg", "gif")

  val logger = LoggerFactory.getLogger(getClass)

  def findEndDelimiter(text: String, start: Int): Int = {
    var index = start
    while(index < text.size) {
      var ch = text.charAt(index)
      if (ch == '"' || ch == '\'' || ch == '<' || ch == '>' || ch.isWhitespace)
        return index
      index += 1
    }

    index;
  }

  def findUrls(text: String): Set[URL] = {

    val Pattern = """http://|https://""".r

    val matches =  Pattern.findAllIn(text).matchData.toIterable

    //Review: Should be able to use an explicit builder instead of copying over to a set
    matches.map { urlMatch =>
      val foundText = text.substring(urlMatch.start, findEndDelimiter(text, urlMatch.end))
      val urlText =StringEscapeUtils.unescapeHtml4(foundText)
      new URL(urlText) }.toSet
  }

  def getPathExtension(url: URL): String = {

    val path = url.getPath
    if (path == null || path.isEmpty)
      return ""

    val index = path.lastIndexOf('.')
    if (index == -1)
      ""
    else
      path.substring(index + 1)
  }

  def okToSkip(url: URL): Boolean = {
    whitelist.contains(url.getHost) || extensionWhitelist.contains(getPathExtension(url))
  }

  def findSpamLink(urls: Set[URL], dnsbl: DnsblLookup): DnsblResult = {

    val suspectUrls = urls.filterNot(okToSkip)
    for(url <- suspectUrls) {

      logger.debug("checking dnsbl: {}", url)
      val record = dnsbl.checkDNBSL(url.getHost)
      if (record != DnsblResult.empty)
        return record
    }

    DnsblResult.empty
  }

}
