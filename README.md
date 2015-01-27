#Emailscript

Hack your inbox! Write simple scripts to do powerful things.

##Features

* Continuously scan folders, to instantly handle emails as they come in
* Tag senders to build whitelists, blacklists, etc.
* Integrates with GoogleContacts
* Move, delete emails
* Send emails using mustache templates
* Currently supports javascript, groovy, and ruby. It is easy to add support for more.

##Example

This is all you need to do to set up a blacklist.

Any email that is dragged into it the "Junk" folder will
cause the sender to be blacklisted. All subsequent blacklisted emails will be moved immediately out of the Inbox.

    // Continuously scan the "Junk" folder for new mails

    MyEmail.scanFolder("Junk"){emails ->    // This closure is called when new emails appear

      for(email in emails){
        if (!email.from.hasTag("blacklisted")){
          logger.info("Blacklisting; from: ${email.from}")
          email.from.addTag("blacklisted")
        }
      }
    }

    // Continuously scan the "Inbox" folder for new mails

    MyEmail.scanFolder("Inbox"){emails ->  // This closure is called when new emails appear

      for(email in emails){
        if (email.from.hasTag("blacklisted")){
          logger.info("$email.from is blacklisted")
          email.moveTo("Junk")
        }
      }
    }

##Documentation

[Getting started, Tutorials] (https://github.com/OdysseusLevy/emailscript/wiki)

[Api documentation](http://odysseuslevy.github.io/emailscript/docs/index.html#package)

##Try it!

Writing email scripts is surprisingly fun. You can do an amazing amount with very little scripting.

Try writing some scripts. If you come up with something useful share it with everyone.

Send your comments/scripts to odysseus-at-cosmosgame.org. Put "Emailscript" somewhere in the subject line.

##Versions

* 0.2.0 1/19/2015 Samples, Documentation
* 0.1.0 Initial Release

