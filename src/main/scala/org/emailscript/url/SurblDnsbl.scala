package org.emailscript.url

import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.io.Source

case class Subdomains(two: String = "", three: String = "", four: String = "")

object SurblDnsbl {
  val logger = Logger(LoggerFactory.getLogger(getClass))
  val SurblHost = "multi.surbl.org"

  def concat(subs: Array[String], count: Int): String = {

    if (subs == null || count > subs.size)
      return ""

    var i: Int = subs.size - count
    var result: String = ""

    while(i < subs.size) {
      result += subs(i)
      i += 1
      if (i < subs.size)
        result += "."
    }

    result
  }

  def apply(twoSet: Set[String] = TwoTLDsSet, threeSet:Set[String] = ThreeTLDsSet) = {
    new SurblDnsbl(twoSet, threeSet)
  }

  def getSubdomains(name: String): Subdomains= {

    val subs = name.split('.')
    Subdomains(concat(subs,2), concat(subs,3), concat(subs,4))
  }

  def loadResource(name: String): Iterator[String] = {
    try {
      Source.fromURL(getClass.getResource("/" + name)).getLines()
    } catch {
      case e: Throwable => logger.error(s"Error loading resource file: $name", e); throw e
    }

  }

  val TwoTLDsSet: Set[String] = loadResource("surbl-two-level-tlds.txt").toSet
  val ThreeTLDsSet: Set[String] = loadResource("surbl-three-level-tlds.txt").toSet
}

class SurblDnsbl(twoSet: Set[String], threeSet: Set[String]) extends DnsblLookup {

  override def getLookupName(host: String): String = {
    getName(host) + "." + SurblDnsbl.SurblHost
  }

  override def getDescriptionForResult(result: String): String = {
    result match {
      case "127.0.0.2" => "SpamCop"
      case "127.0.0.4" => "SpamAssasin"
      case "127.0.0.8" => "Possible phishing site"
      case "127.0.0.16" => "Possible malware site"
      case "127.0.0.32" => "AbuseButler"
      case "127.0.0.64" => "jwSpamSpy"
      case "127.0.0.68" => "jwSpamSpy + SpamAssassin"
      case _ => "unrecognized result"
    }
  }

  def getName(name: String): String = {

    val subs: Subdomains = SurblDnsbl.getSubdomains(name)

    if (!subs.four.isEmpty && threeSet.contains(subs.three)) {
      subs.four
    }
    else if (!subs.three.isEmpty && twoSet.contains(subs.two)) {
      subs.three
    }
    else {
      subs.two
    }

  }

}


