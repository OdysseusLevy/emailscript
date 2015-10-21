Helper.requires(["MyEmail","MailRules"])

//
// Spam
//

def spamFolder = MailRules.SpamFolder ?: "Spam"

// Check for spam

if (spamFolder) {

    MyEmail.scanFolder(spamFolder){ emails ->
        for (email in emails) {
            email.from.removeTag("whitelist")
            email.from.setValue("Category", spamFolder)
        }
    }
}

//
// Sent message recipients are whitelisted
//

MyEmail.scanFolder("Sent"){ emails ->
    logger.info("scanning sent emails")

    for(email in emails) {
        def sentTo = email.to + email.cc + email.bcc
        logger.info("mail sent to $sentTo, subject ${email.subject}")

        for (who in sentTo){
            who.addTag("Sent")
            who.addTag("whitelist")
        }
    }
}

//
// Inbox
//

def getRuleName(email) {
    if (MailRules.CategoryRules) {
        for(rule in MailRules.CategoryRules) {
            if (rule.subjectContains && email.subject.toLowerCase().contains(rule.subjectContains.toLowerCase()))
                return rule.category
            if (rule.host && email.from.host == rule.host)
                return rule.category
        }
    }

    email.from.getValue("Category")
}

def logReason(email, explanation) {
    logger.info("Inbox; from: $email.from subject: $email.subject; $explanation, uid: $email.uid")
}

def whitelist(email) {

    if (!email.from.hasTag("whitelist")) {
        logger.info("Adding ${email.from} to whitelist")
        email.from.addTag("whitelist")
    }

}
MyEmail.scanFolder("Inbox"){emails ->

    for(email in emails){

        def ruleName = getRuleName(email)
        def rule = MailRules.Categories[ruleName]

        // 1: Check for mails manually moved back into Inbox

        if (email.moveHeader) {
            logReason(email, "Manually moved back into Inbox from ${email.moveHeader}")
            whitelist(email)
            email.from.setValue("Category", "")
        }

        // 2: Check for an applicable rule

        else if (rule){
            logReason(email, "using rule, $ruleName, keeping in inbox")
            whitelist(email)
        }

        // 3: Check for whitelist

        else if (email.from.hasTag("whitelist")){
            logReason(email, "sender is explicitly whitelisted, keeping in inbox")
        }

        // 4: None of the above. Figure out where to move it

        else {
            if (MailRules.BulkFolder && email.isVerifiedHost) {
                logReason(email, "does not match any rules, not explicitly whitelisted, sender looks legit, moving to Bulk folder")
                email.moveTo(MailRules.BulkFolder)
            }
            else{
                logReason(email, "does not match any rules, not explicitly whitelisted, sender maybe not legit, moving to Spam folder")
                email.moveTo(spamFolder)
            }

        }

    }
}