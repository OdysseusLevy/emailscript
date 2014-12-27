
def scanFolder(folder, shouldWhiteList) {

    cosmosgame.scanFolder(folder){ emails ->

        if (folder == "Growth" && emails.size() > 10)
            emails = emails[0..10]

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

scanFolder("Social", true)
scanFolder("Notifications", true)
scanFolder("Newsletters", true)
scanFolder("Growth", true)
scanFolder("Archive", true)
scanFolder("Heartbeat", true)
scanFolder("Bulk", false)
scanFolder("Junk", false)

cosmosgame.scanFolder("Sent"){ emails ->
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

cosmosgame.scanFolder("Inbox"){emails ->

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



