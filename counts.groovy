import org.emailscript.Template

class Count {
    static linecount = 0

    def from
    def count
    def read
    def size
    def isFriend
    def backgroundColor() {
        (linecount++ % 2 == 0)? "#cef;":"#fff"
    }

    def sizeInBytes() { Template.toBytes(size)}
    def percentRead() { Template.toPercent(read,count)}

    def String toString() {" count: ${count} read: ${read}"}
}

def folder = "Inbox"
def emails = olevy.getEmails(folder)

def counts = [:]

def increment(counts, email) {
    def currentCount = counts[email.from()] ?: new Count(from: email.from().toString(), count:0,read:0, isFriend: GoogleContacts.contains(email.from()), size:0)
    currentCount.count++
    currentCount.size += email.size()
    if (email.isRead())
        currentCount.read++
    counts[email.from()] = currentCount
}

println ("found ${emails.length} emails")

for ( email in emails) {
    //logger.info("from ${email.from()} subject: ${email.subject()}")
    increment(counts, email)
}

def sorted = counts.sort {-it.value.count}
def result = sorted.values().toList()

if (result.size() > 200) {
    logger.info("truncating to 200")
    result = result[0..199]
}

def html = Template.execute("table.ms", [folder: folder, size: emails.size(), counts:result])

def mail = cosmosgame.newMail()
mail.setSubject("Counts")
mail.setHtml(html)
mail.setFrom("Odysseus The Legend", "odysseus@cosmosgame.org")
mail.setTo("Odysseus The Man", "odysseus@cosmosgame.org")

cosmosgame.send(mail)