Helper.requires(["MyEmail","MailRules"])

//
// Keeps my inbox clear of junk
// Uses MailRules.yaml to share info with daily.groovy
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

// This logs to an external JSON database (Solr). I can then easily see what decisions were made for a given
// email

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

def checkFolderRules(email) {

    def folderName = getFolderName(email)
    if (folderName && MailRules.Folders[folderName]){
        if (MailRules.Folders[folderName].whitelist){
            whiteList(email)
        }
    }
}

def getFolderName(email) {
    for(rule in MailRules.Rules){
        if (rule.subjectContains && email.subject.contains(rule.subjectContains) )
            return rule.folder
        else if (rule.host && email.from.host == rule.host)
            return rule.folder
    }

    email.from.getValue("Folder")
}

MyEmail.scanFolder("Inbox"){email ->

    // First check our rules to see if we should whitelist the sender

    checkFolderRules(email)

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