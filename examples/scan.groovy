Helper.requires(["MyEmail","MailRules"])

//
// Keeps inboxes clear of junk
// Uses the MailRules to configure share info such as folder names across scripts
//


//
// For Gmail account just do a simple blacklist
//

// Continuously scan the "Inbox" folder for new mails

Gmail.scanFolder("Inbox"){email -> // This closure is called whenever we get new mail (or run for first time)

    if (email.moveHeader) {
        logger.info("Mail manually moved back to Inbox; from: ${email.from} subject: ${email.subject}")
        email.from.removeTag("blacklisted")
    }
    if (email.from.hasTag("blacklisted")){
        logger.info("$email.from is blacklisted")
        email.moveTo("Junk")
    }
}

//
// Main email account is more complex
//

//
// Spam
//

MyEmail.scanFolder(MailRules.SpamFolder){ email ->

    if (email.moveHeader != MailRules.SpamFolder) {// manually moved to this folder
        logReason(email, "Manually moved to Spam folder")
        email.from.setValue("folder", MailRules.SpamFolder)
    }
}

//
// Sent message recipients are whitelisted
//

MyEmail.scanFolder("Sent"){ email ->
    def sentTo = email.to + email.cc + email.bcc

    logReason(email, "mail sent to $sentTo, whitelisting them")

    for (who in sentTo){
        who.addTag("Sent")
        who.setValue("folder","Inbox")
    }
}

//
// Inbox
//

def logReason(email, explanation) {
    Notes.indexNote(email, explanation)
    logger.info("Inbox; from: $email.from subject: $email.subject; $explanation, uid: $email.uid")
}

def blacklist(email) {
    email.from.removeTag("whitelist")
    email.from.addTag("blacklist")
}

def whitelist(email) {

    if (!email.from.hasTag("whitelist")) {
        logger.info("Adding ${email.from} to whitelist")
        email.from.addTag("whitelist")
        email.from.removeTag("blacklist")
    }
}

def checkCategory(email) {

    def category = getCategory(email)
    if (category && MailRules.Categories[category]){
        if (MailRules.Categories[category].whitelist && !email.from.getValue("folder") != "Inbox"){
            logReason(email, "whitelisting sender because it is in category $category")
            email.from.addTag("whitelist")
        }
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

MyEmail.scanFolder("Inbox"){email ->

    // First check our rules to see if we should whitelist the sender

    checkCategory(email)

    // 1: Check for mails manually moved back into Inbox

    if (email.moveHeader) {
        logReason(email, "Manually moved back into Inbox from ${email.moveHeader}, whitelisting")
        whitelist(email)
    }

    // 2: Check for whitelisted

    else if (email.from.hasTag("whitelist")){
        logReason(email, "sender is explicitly whitelisted, keeping in inbox")
    }

    // 3: Check for blacklisted

    else if (email.from.hasTag("blacklist")){
        logReason(email, "sender is explicitly blacklist, moving to Spam")
        email.moveTo(MailRules.SpamFolder)
    }
        
    // 4: None of the above. Figure out where to move it

    else {
        if (email.isVerifiedHost) {
            logReason(email, "does not match any rules, not explicitly whitelisted, sender looks legit, moving to Bulk folder")
            email.moveTo(MailRules.BulkFolder)
        }
        else{
            logReason(email, "does not match any rules, not explicitly whitelisted, sender maybe not legit, moving to Spam folder")
            email.moveTo(MailRules.SpamFolder)
        }

    }

}