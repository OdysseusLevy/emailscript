Helper.who("Tagged@taggedmail.com", "Tagged").addTag("blacklist")
Helper.who("info@twitter.com", "Twitter").addTag("blacklist")

Gmail.process{ email->
  logger.info("from: $email.from subject $email.subject")
  if (email.from.hasTag("blacklist"))
    email.delete()
}