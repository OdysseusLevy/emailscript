import org.emailscript.api.Helper

def emails = MyEmail.getEmails()
for(email in emails){
  logger.info("weeks ago: ${email.weeksAgo} days ago: ${email.daysAgo} from: ${email.from} subject: ${email.subject} size: ${Helper.toBytes(email.size)}")
}
