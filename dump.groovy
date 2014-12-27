def emails = cosmosgame.getEmails("Inbox",10)

println ("found ${emails.length} emails")

for ( email in emails) {
    logger.info("*****from ${email.from()} subject: ${email.subject()}")
    email.dumpStructure()
    logger.info("body: ${email.body()}")
    logger.info("******")
}