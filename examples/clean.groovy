Helper.who("Tagged@taggedmail.com", "Tagged").addTag("blacklist")

Gmail.foreach{ email->
  logger.info("from: $email.from subject $email.subject")
  if (email.from.hasTag("blacklist"))
    email.delete()
}