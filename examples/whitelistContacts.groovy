
for( who in GoogleContacts.emails) {

  if (!who.hasTag("whitelist")) {
    logger.info("Adding to whitelist: $who")
    who.addTag("whitelist")
  }
  else{
    logger.info("already whitelisted: $who")
  }
}