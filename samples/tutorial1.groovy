def emails = MyEmail.getEmails(10)
for(email in emails){
  logger.info("from: ${email.from()} subject: ${email.subject()}
}
