def emails = MyEmail.getEmails(10)
for(email in emails){
  println("from: ${email.from} subject: ${email.subject}")
}
