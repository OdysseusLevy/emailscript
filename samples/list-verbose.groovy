def emails = MyEmail.getEmails(10)
for(email in emails){
  println("weeks ago: ${email.weeksAgo} days ago: ${email.daysAgo} from: ${email.from} subject: ${email.subject}")
}
