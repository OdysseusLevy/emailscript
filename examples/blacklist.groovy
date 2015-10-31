// Continuously scan the "Junk" folder for new mails

Gmail.scanFolder("Junk"){email ->    // This closure is called whenever we get new mail (or run for first time)

    if (!email.from.hasTag("blacklisted")){
        logger.info("Blacklisting; from: ${email.from}")
        email.from.addTag("blacklisted", true)
    }
}

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