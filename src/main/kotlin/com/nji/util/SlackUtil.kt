package com.nji.util

import com.nji.conf.SlackSubfield
import com.ullink.slack.simpleslackapi.SlackAttachment
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory


class SlackUtil {


    companion object {

        val logger = LoggerFactory.getLogger(SlackUtil::class.java)

        /**
         * Send issues to Slack.
         * @property token the issues that are stored locally.
         * @property channels the issues returned by the Jira filter.
         * @property fields the issues detected as new.
         * @property priorityName the name of the tag element that represents priority in JIRA.
         * @property priorities the list of priorities in JIRA.
         * @property newIssuesMap a list of new issues.
         */
        fun sendIssuesToSlack(
                token: String,
                channels: List<String>,
                colors: List<String>,
                fields: MutableMap<String, String>,
                subFields: MutableMap<String, SlackSubfield>,
                priorityName: String,
                priorities: List<String>,
                newIssuesMap: MutableMap<String, MutableMap<String, String>>) {

            val session = SlackSessionFactory.createWebSocketSlackSession(token)

            if (!session.isConnected) {
                session.connect()
            }

            priorities.forEach {
                newIssuesMap.forEach { issue ->
                    val priorityIssue = issue.value[priorityName]
                    if (it == priorityIssue) {
                        val position = priorities.indexOf(priorityIssue)
                        val channel = session.findChannelByName(channels[position])
                        val color = colors[position]
                        session.sendMessage(channel, buildSlackMessage(fields, subFields, issue.value, color))
                        logger.info("Message sent to Slack: {}", issue.key)

                    }
                }
            }

            session.disconnect()
        }

        /**
         * Build the Slack message.
         * @property fields the list of fields to be sent to Slack.
         * @property issue the data of a JIRA issue.
         * @return message a Slack message.
         */
        private fun buildSlackMessage(
                fields: MutableMap<String, String>,
                subFields: MutableMap<String, SlackSubfield>,
                issue: MutableMap<String, String>,
                color: String): SlackPreparedMessage? {

            val slackAttachment = SlackAttachment()
            slackAttachment.color = color
            slackAttachment.title = issue[fields["slack-message.title"]]
            slackAttachment.titleLink = issue[fields["slack-message.title-link"]]

            // Description is too long for Slack so it could not be suitable
            if (fields["slack-message.text"] != null && issue[fields["slack-message.text"]] != null) {
                val textFormatted = Jsoup.parse(issue[fields["slack-message.text"]]).text()
                slackAttachment.text = textFormatted
            }

            for (subField in subFields.values) {
                slackAttachment.addField(subField.title, issue[subField.value], subField.short)
            }

            //build a message object
            return SlackPreparedMessage.Builder()
                    .withUnfurl(false)
                    .addAttachment(slackAttachment)
                    .build()
        }
    }
}