logger.error("test", new Exception("boom!"))
MyEmail.foreach([197881, 202438]){ email ->
//[197881, 202438]
    logger.info("uid: ${email.uid} from: ${email.from} subject: ${email.subject}")
  Emails.indexEmail(email)
     Notes.indexNote(email, "verified host: ${email.verifiedHost}")
//    email.dumpStructure()
//    logger.info("html $email.body")
//    email.attachments.each{ attachment -> logger.info("file: ${attachment.fileName}")}
}
