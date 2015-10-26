Helper.requires(["MyEmail","MailRules"])

//
// Keeps inbox clear of junk
// Uses the MailRules to configure share info such as folder names across scripts
//

//
// Spam
//

MyEmail.scanFolder(MailRules.SpamFolder){ email ->

    if (email.moveHeader != MailRules.SpamFolder) {// manually moved to this folder
        blacklist(email)
        logReason(email, "Manually moved to Junk folder")
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
        who.addTag("whitelist")
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
    }

}
MyEmail.scanFolder("Inbox"){email ->

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
        
    // 4: Check if we want to whitelist this email
    else if (email.subject.contains("Emailscript") ||
            email.subject.contains("Jaiya") ||
            email.subject.contains("Uly")){
        logReason(email, "Subject contains pass phrase, whitelisting sender")
        whitelist(email)
    }

    // 5: None of the above. Figure out where to move it

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