def emails = MyEmail.getEmails()
for(email in emails){
  println("from: ${email.from} subject: ${email.subject}")
}
