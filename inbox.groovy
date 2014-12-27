
cosmosgame.scanFolder("Inbox"){ emails ->
    def whiteList = Data.getOrElse("okContacts", []) as Set
    def laterList = Data.getOrElse("laterContacts", []) as Set
    def junkList = Data.getOrElse("junkContacts", []) as Set


    for(email in emails) {
        def from = email.from()

        if (laterList.contains(from))
            email.moveTo("Later")
        else if (junkList.contains(from))
            email.moveTo("Junk")
        else if (GoogleContacts.contains(from))
            logger.info("${from} in contacts")
        else if (whiteList.contains(from))
            logger.info("${from} in whitelist")
        else if (email.moveHeader()) {
            whiteList += from
            logger.info("Put ${from} into whitelist. Moved from ${email.moveHeader()}")
            Data.set("okContacts", whiteList)
        } else if (email.verifiedHost()) {
            email.moveTo("Bulk")
        } else {
            email.moveTo("Junk")
        }
    }
}