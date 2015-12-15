Helper.requires(["MyEmail","MailRules"])

def folderRules = MailRules.Folders // MailRules is a yml file in the config folder

//
// First visit all of the folders where we have rules defined
//

folderRules.each { folderName, rules -> applyRules(folderName, rules) }

// For a given folder look for mails manually moved into the folder. Also clean out old emails if that is in the
// rules for this folder

def applyRules(folderName, rules) {

    logger.info("Applying rules for folder: $folderName rules: $rules")
    if (rules.skipScan)
        return

	if (!MyEmail.hasFolder(folderName)){
		logger.warn("Configuration Error! Can not find folder: $folderName")
	    return
	}

    // Check for any emails.froms that do not have their "Folder" value set

    MyEmail.readLatest(folderName) { email ->

        if (email.from.getValue("Folder") != folderName){
            logger.info("From: ${email.from} subject: ${email.subject} setting folder to ${folderName}")

            email.from.setValue("Folder", folderName)
            if (rules.whitelist)
                email.from.addTag("whitelist")
        }
    }

    // If the rules have a folderDays property set get rid of any mails older than that

    if (rules.folderDays) {

        def daysAgo = Helper.daysAgo(rules.folderDays)
        logger.info("looking for emails before: $daysAgo in $folderName")

        MyEmail.foreachBefore(folderName, daysAgo){ email ->
            logger.info("removing old mail from: ${email.from} subject: ${email.subject} daysAgo: ${email.daysAgo}")
            email.delete()
        }
    }

}

//
// Next, go through inbox emails looking for mails we should move to a folder
//

def getFolder(email) {
    for(rule in MailRules.Rules){
        if (rule.subjectContains && email.subject.contains(rule.subjectContains) )
            return rule.folder
        else if (rule.host && email.from.host == rule.host)
            return rule.folder
    }

    email.from.getValue("Folder")
}

def counts = [:]
MyEmail.foreachReversed("Inbox"){ email ->

    def folder = getFolder(email)
    if (!folder)
        return

    def rule = folderRules[folder]
    if (!rule)
        return

    counts[email.from] = (counts[email.from] ?: 0) + 1

    if (rule.saveUnread && !email.isRead)
        return

    if (rule.inboxMax && counts[email.from] > rule.inboxMax) {
        logger.info("email from: ${email.from} subject: ${email.subject} count: ${counts[email.from]} > ${rule.inboxMax}")
        email.moveTo(folder)
    }

    if (rule.inboxDays &&  email.daysAgo > rule.inboxDays){
        logger.info("mail from: ${email.from} subject: ${email.subject} daysAgo: ${email.daysAgo} > ${rule.inboxDays}")
        email.moveTo(folder)
    }
}

