#Emailscript

Hack your inbox! Write simple scripts to do powerful things.

##Features

* Continuously scan folders, to instantly handle emails as they come in
* Tag senders to build whitelists, blacklists, etc.
* Integrates with GoogleContacts
* Move, delete emails
* Send emails using mustache templates
* Supports javascript, groovy, or ruby

##Example

This script is all that is needed to set up a blacklist.

Any email that is dragged into it the "Junk" folder will
cause the sender to be blacklisted. All blacklisted emails will be moved immediately out of the Inbox.

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

##Documentation

[Getting started, Tutorials] (https://github.com/OdysseusLevy/emailscript/wiki)

[Api documentation](http://odysseuslevy.github.io/emailscript/docs/index.html#package)

##Please try it!

I would love to get feedback!

Please send me your scripts. I need more samples!

Send your comments/scripts to odysseus-at-cosmosgame.org. Put "Emailscript" somewhere in the subject line.

##Versions

* 0.2.0 1/19/2015 Samples, Documentation
* 0.1.0 Initial Release

