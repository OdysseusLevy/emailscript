def getRuleName(email) {
    if (MailRules.CategoryRules) {
        for(rule in MailRules.CategoryRules) {
            if (rule.subjectContains && email.subject.toLowerCase().contains(rule.subjectContains.toLowerCase()))
                return rule.category
            if (rule.host && email.from.host == rule.host)
                return rule.category
        }
    }

    email.from.getValue("Category") ?: ""
}

def emails = MyEmail.getEmails()

for(email in emails){

    def ruleName = getRuleName(email)
    def rule = MailRules.Categories[ruleName]

    if (ruleName == "Junk"){
        email.from.addTag("whitelist")
        println("clearing category")
        email.from.setValue("Category", "")
    }
    if (rule)
        println("Rule found: $ruleName; from: ${email.from} host: $email.from.host subject: ${email.subject}")

}