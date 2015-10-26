// Continuously scan the "Junk" folder for new mails

MyEmail.scanFolder("Junk"){email ->    // This closure is called whenever we get new mail (or run for first time)

    if (!email.from.hasTag("blacklisted")){
        logger.info("Blacklisting; from: ${email.from}")
        email.from.addTag("blacklisted", true)
    }
}

// Continuously scan the "Inbox" folder for new mails

MyEmail.scanFolder("Inbox"){email -> // This closure is called whenever we get new mail (or run for first time)

     if (email.from.hasTag("blacklisted")){
        logger.info("$email.from is blacklisted")
         email.moveTo("Junk")
     }
}