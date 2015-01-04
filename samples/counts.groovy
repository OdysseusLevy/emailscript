import org.emailscript.Template

class Count {
    static linecount = 0

    Count(email) {
        from = email.from().toString()
    }
    def from
    def count = 0
    def read = 0
    def size = 0
    def backgroundColor() {
        (linecount++ % 2 == 0)? "#cef;":"#fff"
    }

    def sizeInBytes() { Template.toBytes(size)}
    def percentRead() { Template.toPercent(read,count)}

    def String toString() {" count: ${count} read: ${read}"}
}

def folder = "Inbox"
def emails = MyEmail.getEmails(folder)
def counts = [:]

def increment(counts, email) {
    def currentCount = counts[email.from()] ?: new Count(email)
    currentCount.count++
    currentCount.size += email.size()
    if (email.isRead())
        currentCount.read++
    counts[email.from()] = currentCount
}

logger.info ("found ${emails.length} emails")

for ( email in emails) {
    increment(counts, email)
    logger.info("from ${email.from()} subject: ${email.subject()} counts: ${counts[email.from()]}")
}

def sorted = counts.sort {-it.value.count} //sort from highest to lowest
def result = Template.execute("table.ms", [folder: folder, size: emails.size(), counts:sorted.values()])

def mail = MyEmail.newMail()
mail.setSubject("Counts")
mail.setHtml(result)
mail.setTo("me@mymail.com")

MyEmail.send(mail)