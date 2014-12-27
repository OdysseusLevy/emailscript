def mail = cosmosgame.newMail()
mail.setSubject("Emailscript rules!!")
mail.setHtml("<h1>Happiness?</h1><b>Woohoo!!</h2>")
mail.setFrom("Odysseus The Legend", "odysseus@cosmosgame.org")
mail.setTo("Odysseus The Man", "odysseus@cosmosgame.org")

cosmosgame.send(mail)