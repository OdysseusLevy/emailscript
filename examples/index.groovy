def emails = MyEmail.getEmails()
for(email in emails){
    logger.info("uid: ${email.uid} from: ${email.from} subject: ${email.subject}")
    Search.index(email)
}
