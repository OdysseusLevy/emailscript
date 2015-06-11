def emails = Search.search("*:* AND !phch", 100)
for(email in emails) {
    logger.info("from: $email.from subject: $email.subject")
}