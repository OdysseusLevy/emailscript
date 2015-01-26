MyEmail.scanFolder("Junk", false){emails ->

    for(email in emails){
        if (!email.from.getValue("blacklisted")){
            logger.info("Blacklisting; from: ${email.from}")
            email.from.setValue("blacklisted", true)
        }
    }
}

MyEmail.scanFolder("Inbox"){emails ->

    for(email in emails){
         if (email.from.getValue("blacklisted")){
            logger.info("$email.from is blacklisted")
             email.moveTo("Junk")
         }
    }
}