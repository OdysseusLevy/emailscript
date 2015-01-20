
def scanFolder(folder, shouldWhiteList) {

    if (!MyEmail.hasFolder(folder)) {
        logger.info("Ignoring scanFolder request for $folder")
        return
    }

    MyEmail.scanFolder(folder){ emails ->

        logger.info("scanning mail in $folder")
        for(email in emails){
            logger.info("from ${email.from()} subject: ${email.subject()}")
            if (email.moveHeader() != folder) {
                logger.info("Manually moved into folder from  ${email.moveHeader()}; setting category to: ${folder}, Whitelist?: ${shouldWhiteList}")
                email.from().setValue("Category", folder)
                if (shouldWhiteList)
                    email.from().setTag("Whitelist")
                else
                    email.from().removeTag("Whitelist")
            }
        }
    }
}

scanFolder("Notifications", true)
scanFolder("Newsletters", true)
scanFolder("Archive", true)
scanFolder("Bulk", false)
scanFolder("Junk", false)

MyEmail.scanFolder("Sent"){ emails ->
    logger.info("scanning sent emails")

    for(email in emails) {
        def sentTo = email.to() + email.cc() + email.bcc()
        logger.info("mail sent to $sentTo, subject ${email.subject()}")

        for (who in sentTo){
            who.setTag("Sent")
            who.setTag("Whitelist")
        }
    }
}

MyEmail.scanFolder("Inbox"){emails ->

    for(email in emails){

        def from = email.from()
        def isWhitelisted = from.hasTag("Whitelist")

        logger.info("Inbox; from: $from subject: ${email.subject()} whitelist?: ${isWhitelisted}")

        // Check for mails manually moved back into Inbox

        if (email.moveHeader()) {
            logger.info("Manually moved into Inbox from  ${email.moveHeader()}")
            from.setTag("Whitelist")
            from.setValue("Category", "")
            return
        }

        if (!isWhitelisted){
            if (email.from().getValue("Category"))
                email.moveTo(email.from.getValue("Category"))
            else if (email.verifiedHost())
                email.moveTo("Bulk")
            else
                email.moveTo("Junk")
        }
    }
}



