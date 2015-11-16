MyEmails.foreach(){email ->
  logger.info("weeks ago: ${email.weeksAgo} days ago: ${email.daysAgo} from: ${email.from} subject: ${email.subject} " +
          "is read?: ${email.isRead} size: ${Helper.toBytes(email.size)}")
}
