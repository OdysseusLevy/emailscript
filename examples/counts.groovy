
class Count {
    static linecount = 0

    Count(email, formatHelper) {
        from = email.from.toString()
        helper = formatHelper  // Global script variables like Helper are not accessible inside a class
    }

    def helper
    def from
    def count = 0
    def read = 0
    def size = 0
    def backgroundColor() {
        (linecount++ % 2 == 0)? "#cef;":"#fff"
    }

    def sizeInBytes() { helper.toBytes(size)}
    def percentRead() { helper.toPercent(read,count)}

    def String toString() {" count: ${count} read: ${read}"}
}

def counts = [:]

def increment(counts, email) {
    def currentCount = counts[email.from] ?: new Count(email, Helper)
    currentCount.count++
    currentCount.size += email.size
    if (email.isRead)
        currentCount.read++
    counts[email.from] = currentCount
}

//
// Scan through all emails in Inbox
//

def folder = "Inbox" // change this if you want to scan a different folder
def total = 0

MyEmail.foreach(folder){ email ->
    total++
    increment(counts, email)
}

def sorted = counts.sort {-it.value.count} //sort from highest to lowest
sorted = sorted.values().toList()

def result = sorted.size() <= 200 ? sorted : sorted[0..199] // truncate if too large

def html = Helper.execute("table.ms", [folder: folder, size: total, counts: result])

def mail = MyEmail.newMail()
mail.setSubject("Counts")
mail.setHtml(html)
//mail.setTo("myemail@myhost.com")  // Set this if you want to send to a different email address

MyEmail.send(mail)