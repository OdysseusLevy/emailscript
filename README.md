#Emailscript

Hack your inbox! Write simple scripts to tame your mail.

##Features

* Continuously scan folders, to instantly handle emails as they come in
* Tag senders to build whitelists, blacklists, etc.
* Full DKIM support to validate emails as legitimate
* Integrates with GoogleContacts
* Move, delete emails
* Send emails using mustache templates
* Currently supports javascript, groovy, and ruby. It is easy to add support for more.

##Example

This is all you need to do to set up a blacklist.

Any email that is dragged into the "Junk" folder causes the sender to be blacklisted. All subsequent blacklisted
emails will be moved immediately out of the Inbox. If
you change your mind and drag an email back into the Inbox, that will remove the sender from the blacklist

    // Continuously scan the "Junk" folder for new mails

    MyEmail.scanFolder("Junk"){email ->    // This closure is called when new emails appear
          logger.info("Blacklisting; from: ${email.from}")
          email.from.addTag("blacklisted")
    }

    // Continuously scan the "Inbox" folder for new mails

    MyEmail.scanFolder("Inbox"){email ->  // This closure is called when new emails appear

        if (email.moveHeader) // email was manually moved back to Inbox
            email.from.removeTag("blacklisted")

        if (email.from.hasTag("blacklisted")){
          logger.info("$email.from is blacklisted")
          email.moveTo("Junk")
        }
    }

To run this script you just need the emailscript jar

    java -jar emailscript.jar scan.groovy

Again, note that scripts can be in either groovy, ruby, or javascript. If you want more, ask! They are easy to add.

##Documentation

[Getting started, Tutorials] (https://github.com/OdysseusLevy/emailscript/wiki)

[Api documentation](http://odysseuslevy.github.io/emailscript/docs/index.html#package)

##Try it!

Writing email scripts is surprisingly fun. You can do an amazing amount with very little scripting.

Try writing some scripts. If you come up with something useful share it with everyone.

Send your comments/scripts to odysseus-at-cosmosgame.org. Put "Emailscript" somewhere in the subject line.

##Versions

* 0.3.2 Fixed memory leak, Folder api
* 0.3.1 Dkim fixups
* 0.3.0 11/22/2015 DKIM, api improvements, export to external DB via JSON (eg. Solr)
* 0.2.1 1/26/2015 Bug fixes, more samples
* 0.2.0 1/19/2015 Samples, Documentation
* 0.1.0 Initial Release

