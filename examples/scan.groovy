Helper.requires(["MyEmail","MailRules"])

//
// Spam
//

def spamFolder = MailRules.Spam?.folder

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

def getCategoryName(email) {
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

def isBlacklisted(category, email) {
    if (category)
        category.blacklist == true
    else if (MailRules.Hardcore)
        !email.from.hasTag("whitelist")
    else
        email.from.hasTag("blacklist")
}

MyEmail.scanFolder("Inbox"){emails ->

    for(email in emails){

        def categoryName = getCategoryName(email)
        def category = MailRules.Categories[categoryName]

        def isBlacklisted = isBlacklisted(category, email)

        if (MailRuiles.Hardcore&& !isBlacklisted && !email.from.hasTag("whitelist")){
            logger.info("whitelisting $email.from")
            email.from.addTag("whitelist")
        }

        logger.info("Inbox; from: $email.from subject: ${email.subject} category: $categoryName blacklisted?: ${isBlacklisted}")

        // Check for mails manually moved back into Inbox

        if (email.moveHeader) {
            logger.info("Manually moved back into Inbox from  ${email.moveHeader}")
            email.from.addTag("whitelist")
            email.from.setValue("Category", "")
        }
        else if (isBlacklisted){
            if (category)
                email.moveTo(categoryName)
            else if (MailRules.BulkFolder && email.isVerifiedHost)
                email.moveTo(MailRules.BulkFolder)
            else
                email.moveTo(spamFolder)
        }

    }
}