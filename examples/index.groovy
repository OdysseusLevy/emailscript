logger.error("test")
def emails = MyEmail.getEmails()
//[197881, 202438]
for(email in emails){
    logger.info("uid: ${email.uid} from: ${email.from} subject: ${email.subject}")
  Emails.indexEmail(email)
     Notes.indexNote(email, "verified host: ${email.verifiedHost}")
//    email.dumpStructure()
//    logger.info("html $email.body")
//    email.attachments.each{ attachment -> logger.info("file: ${attachment.fileName}")}
}
