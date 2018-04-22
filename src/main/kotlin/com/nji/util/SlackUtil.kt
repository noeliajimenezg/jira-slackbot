package com.nji.util

import com.nji.conf.SlackSubfield
import com.ullink.slack.simpleslackapi.SlackAttachment
import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

class SlackUtil{


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
                subfields: MutableMap<String, SlackSubfield>,
                priorityName: String,
                priorities: List<String>,
                newIssuesMap: MutableMap<String, MutableMap<String, String>>) {

            val session : SlackSession = SlackSessionFactory.createWebSocketSlackSession(token)
            session.connect()

            for(priority in priorities){
                for(issue in newIssuesMap){
                    val priorityIssue = issue.value[priorityName]
                    if (priority == priorityIssue){
                        val position = priorities.indexOf(priorityIssue)
                        val channel : SlackChannel = session.findChannelByName(channels[position])
                        val color : String = colors[position]
                        session.sendMessage(channel, buildSlackMessage(fields, subfields, issue.value, color))
                        logger.info("Message sent to Slack: {}", issue.key)

                    }
                }
            }
        }



        /**
         * Build the Slack message.
         * @property fields the list of fields to be sent to Slack.
         * @property issue the data of a JIRA issue.
         * @return message a Slack message.
         */
        private fun buildSlackMessage(
                fields: MutableMap<String, String>,
                subfields: MutableMap<String, SlackSubfield>,
                issue: MutableMap<String, String>,
                color: String): SlackPreparedMessage? {

            val slackAttachment = SlackAttachment()
            slackAttachment.color = color
            slackAttachment.title = issue[fields["slack-message.title"]]
            slackAttachment.titleLink = issue[fields["slack-message.title-link"]]
            // Description is too long for Slack
            if (fields["slack-message.text"] != null) {
                val textFormatted = Jsoup.parse(issue[fields["slack-message.text"]]).text()
                slackAttachment.text = textFormatted
            }

            for(subfield in subfields.values){
                slackAttachment.addField(subfield.title, issue[subfield.value], subfield.short)
            }

            //build a message object
            return SlackPreparedMessage.Builder()
                    .withUnfurl(false)
                    .addAttachment(slackAttachment)
                    .build()
        }



    }
}