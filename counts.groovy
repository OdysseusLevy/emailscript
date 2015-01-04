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

def folder = "@SaneArchive"
def emails = cosmosgame.getEmails(folder)
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
    logger.info("from ${email.from()} subject: ${email.subject()}")
    increment(counts, email)
}

def sorted = counts.sort {-it.value.count}

sorted.forEach{ k,v -> println("$k count: $v.count read: $v.read size: $v.size percentRead: ${v.percentRead()} isFriend: ${v.isFriend}")}

def result = Template.execute("table.ms", [folder: folder, size: emails.size(), counts:sorted.values()])
println result

def mail = cosmosgame.newMail()
mail.setSubject("Counts")
mail.setHtml(result)
mail.setFrom("Odysseus The Legend", "odysseus@cosmosgame.org")
mail.setTo("Odysseus The Man", "odysseus@cosmosgame.org")

cosmosgame.send(mail)