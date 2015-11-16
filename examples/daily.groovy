Helper.requires(["MyEmail","MailRules"])

def categories = MailRules.Categories

//
// First visit all of the folders associated with our categories and clear out old mail, etc.
//

categories.each { category, policy -> applyRules(category, policy) }

// For a given folder look for mails manually moved into the folder. Also clean out old emails if that is the
// policy for this folder

def applyRules(folder, policy) {

    logger.info("Applying rules for folder: $folder policy: $policy")
    if (policy.skipScan)
        return

	if (!MyEmail.hasFolder(folder)){
		logger.warn("Configuration Error! Can not find folder: $folder")
	    return
	}
	
    // Check any emails added since last time

    MyEmail.readLatest(folder, { email ->
            if (email.moveHeader != folder) {
                logger.info("Email moved to $folder manually from folder: ${email.moveHeader}, updating info; from: ${email.from} subject: ${email.subject}")

                email.from.setValue("Category", folder)
                if (policy.whitelist)
                    email.from.addTag("whitelist")
                else
                    email.from.removeTag("whitelist")
            }
    })

    // If this policy has a folderDays property set get rid of any mails older than that

    if (policy.folderDays) {

        def daysAgo = Helper.daysAgo(policy.folderDays)
        logger.info("looking for emails before: $daysAgo in $folder")
        emails = MyEmail.getEmailsBefore(folder, daysAgo)
        for(email in emails) {
            logger.info("removing old mail from: ${email.from} subject: ${email.subject} daysAgo: ${email.daysAgo}")
            email.delete()
        }
    }

}

//
// Next, go through inbox emails looking for mails we should move to a folder
//

def counts = [:]
def emails = MyEmail.getEmails("Inbox")
emails = emails.reverse()   //want newest to oldest

for(email in emails){

    def category = getCategory(email)
    if (!category)
        continue

    def rule = categories[category]
    if (!rule)
        continue

    counts[email.from] = (counts[email.from] ?: 0) + 1

    if (rule.saveUnread && !email.isRead)
        continue

    if (rule.inboxMax && counts[email.from] > rule.inboxMax) {
        logger.info("email from: ${email.from} subject: ${email.subject} count: ${counts[email.from]} > ${rule.inboxMax}")
        email.moveTo(category)
    }

    if (rule.inboxDays &&  email.daysAgo > rule.inboxDays){
        logger.info("mail from: ${email.from} subject: ${email.subject} daysAgo: ${email.daysAgo} > ${rule.inboxDays}")
        email.moveTo(category)
    }
}

def getCategory(email) {
    for(rule in MailRules.Rules){
        if (rule.subjectContains && email.subject.contains(rule.subjectContains) )
            return rule.category
        else if (rule.host && email.from.host == rule.host)
            return rule.category
    }

    email.from.getValue("Category")
}
