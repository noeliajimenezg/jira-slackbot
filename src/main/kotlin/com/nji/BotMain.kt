package com.nji


import com.nji.conf.BotConf
import com.nji.conf.JiraConf
import com.nji.conf.SlackConf
import com.nji.util.FileUtil.Companion.readStoredIssues
import com.nji.util.FileUtil.Companion.saveIssuesToFile
import com.nji.util.JiraUtil.Companion.detectNewIssues
import com.nji.util.JiraUtil.Companion.getDataFromJira
import com.nji.util.JiraUtil.Companion.setSystemProperties
import com.nji.util.JiraUtil.Companion.updateStoredIssues
import com.nji.util.SlackUtil.Companion.sendIssuesToSlack
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule


@Component
class BotMain : CommandLineRunner {

    val logger = LoggerFactory.getLogger(BotMain::class.java)

    @Autowired
    lateinit var conf: BotConf

    override fun run(vararg args: String?) {
        val periodSeconds: Long = (conf.jira.minutes * 60 * 1000).toLong()
        logger.info("Let's check the issues every {} minutes between {} and {}", conf.jira.minutes, conf.jira.startTime, conf.jira.endTime)
        Timer(true).schedule(1000, periodSeconds){
            process(conf.jira, conf.slack)
        }
    }

    /**
     * Process the JIRA issues and send the Slack alert
     * @property jira the JIRA configuration.
     * @property slack the Slack configuration.
     */
    private fun process(jira: JiraConf, slack: SlackConf) {
        if (isBetween(jira.startTime, jira.endTime)) {
            logger.info("Checking...")
            // Setting the system properties
            setSystemProperties(jira.proxy, jira.proxyPort, jira.javaHome)
            // Connection and getting the data from JIRA filter
            val issuesFilter = getDataFromJira(jira.username, jira.password, jira.url, jira.filterId, jira.elementTagName, jira.keyElementTagName)
            // Read local stored issues. Each line represents a priority
            val storedIssues = readStoredIssues(jira.localFileStoredIssues)
            // Get the new Jira issues to be sent to Slack
            val newIssues: MutableMap<String, MutableMap<String, String>> = detectNewIssues(issuesFilter, storedIssues)
            // Update the issues in the file
            val storedIssuesUpdated: List<String> = updateStoredIssues(storedIssues, issuesFilter, newIssues, jira.priority, jira.priorityName)
            // Save to file
            saveIssuesToFile(storedIssuesUpdated, jira.localFileStoredIssues)

            // Send to Slack
            if (slack.active && !newIssues.isEmpty()) {
                sendIssuesToSlack(slack.token, slack.channels, slack.colors, slack.fields, slack.subfields, jira.priorityName, jira.priority, newIssues)
            }
            logger.info("Ending check...")
        }
    }

    /**
     * Check if now() is between start and end time.
     * @property startTime the JIRA configuration.
     * @property endTime the Slack configuration.
     * @return Boolean
     */
    private fun isBetween(startTime: String, endTime: String): Boolean {
        val today = Calendar.getInstance()
        val df = SimpleDateFormat("HH:mm")
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        today.time = Date()
        start.time = df.parse(startTime)
        end.time = df.parse(endTime)

        logger.info("Checking {} between {} and {}", df.format(today.time), conf.jira.startTime, conf.jira.endTime)
        return (isEqualOrAfter(today[Calendar.HOUR_OF_DAY], today[Calendar.MINUTE], start[Calendar.HOUR_OF_DAY], start[Calendar.MINUTE])
                && isEqualOrBefore(today[Calendar.HOUR_OF_DAY], today[Calendar.MINUTE], end[Calendar.HOUR_OF_DAY], end[Calendar.MINUTE]))
    }

    /**
     * Check if now() is equal or after start time.
     * @property hour
     * @property minute
     * @property beginHour
     * @property beginMinute
     * @return Boolean
     */
    private fun isEqualOrAfter(hour: Int, minute: Int, beginHour: Int, beginMinute: Int): Boolean {
        return (beginHour < hour || (beginHour == hour && beginMinute <= minute))
    }

    /**
     * Check if now() is equal or before end time.
     * @property hour
     * @property minute
     * @property endHour
     * @property endMinute
     * @return Boolean
     */
    private fun isEqualOrBefore(hour: Int, minute: Int, endHour: Int, endMinute: Int): Boolean {
        return (hour < endHour || (hour == endHour &&  minute <= endMinute))
    }

}