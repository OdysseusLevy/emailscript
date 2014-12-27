print("Hello Nashorn!!!")

var emails = cosmosgame.getEmails("Inbox", 200)

print (emails.getClass)
print (emails.length)


function getEmailCount(email) {

    var count = new Object()
    count.count = 1

    if (email.isRead())
        count.opened = 1
    else
        count.opened = 0

    return count
}

function increment(count1, count2) {

    var result = new Object()

    result.count = count1.count + count2.count
    result.opened = count1.opened + count2.opened

    return result
}

var counts = {}

for(i= 0; i < emails.length; i++ ) {
    var email = emails[i]

    var currentCount
    var from = email.from().email()

    if (from in counts)
       currentCount = counts[from]
    else
       currentCount = {count:0,opened:0}

    counts[from] = increment(currentCount, getEmailCount(email))
    print( from + " " + email.subject() + "is Read? " + email.isRead())
}

for(var from in counts) {
    print (from + " count: " + counts[from].count + " opened: " + counts[from].opened )
}
