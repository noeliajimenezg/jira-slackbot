package com.nji


import com.nji.conf.BotConf
import com.nji.conf.JiraConf
import com.nji.conf.SlackConf
import com.nji.util.FileUtil.Companion.readStoredIssues
import com.nji.util.FileUtil.Companion.saveIssuesToFile
import com.nji.util.JiraUtil.Companion.setSystemProperties
import com.nji.util.JiraUtil.Companion.detectNewIssues
import com.nji.util.JiraUtil.Companion.getDataFromJira
import com.nji.util.JiraUtil.Companion.updateStoredIssues
import com.nji.util.SlackUtil.Companion.sendIssuesToSlack
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.*
import kotlin.concurrent.schedule
import java.text.SimpleDateFormat


@Component
class BotMain : CommandLineRunner {

    val logger = LoggerFactory.getLogger(BotMain::class.java)

    @Autowired
    lateinit var conf: BotConf

    override fun run(vararg args: String?) {
        val periodSeconds: Long = (conf.jira.minutes * 60 * 1000).toLong()
        logger.info("Let's check the issues every {} minutes between {} and {}", conf.jira.minutes, conf.jira.startTime, conf.jira.endTime)
        Timer().schedule(0, periodSeconds) {
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
            // Setting the system properties
            setSystemProperties(jira.proxy, jira.proxyPort, jira.javaHome)
            // Connection and getting the data from JIRA filter
            val issuesFilter = getDataFromJira(jira.username, jira.password, jira.url, jira.filterId, jira.elementTagName, jira.keyElementTagName)
            // Read local stored issues. Each line represents a priority
            val storedIssues = readStoredIssues(jira.localFileStoredIssues)
            // Get the new Jira issues to be sent to Slack
            val newIssues: MutableMap<String, MutableMap<String, String>> = detectNewIssues(issuesFilter, storedIssues)
            // Update the issues in the file
            val storedIssuesUpdated: ArrayList<String> = updateStoredIssues(storedIssues, issuesFilter, newIssues, jira.priority, jira.priorityName)
            // Save to file
            saveIssuesToFile(storedIssuesUpdated, jira.localFileStoredIssues)

            // Send to Slack
            if (slack.active && !newIssues.isEmpty()) {
                sendIssuesToSlack(slack.token, slack.channels, slack.colors, slack.fields, slack.subfields, jira.priorityName, jira.priority, jira.proxy, jira.proxyPort, newIssues)
            }
        }
    }

    /**
     * Check if now() is between start and end time.
     * @property startTime the JIRA configuration.
     * @property endTime the Slack configuration.
     * @return Boolean
     */
    private fun isBetween(startTime: String, endTime: String): Boolean {
        val today: Calendar = Calendar.getInstance()
        val df = SimpleDateFormat("HH:mm")
        val start: Calendar = Calendar.getInstance()
        val end: Calendar = Calendar.getInstance()
        today.time = Date()
        start.time = df.parse(startTime)
        end.time = df.parse(endTime)

        logger.info("Checking {} between {} and {}", df.format(today.time), conf.jira.startTime, conf.jira.endTime)
        if (start.get(Calendar.HOUR_OF_DAY) <= today.get(Calendar.HOUR_OF_DAY)
                && start.get(Calendar.MINUTE) <= today.get(Calendar.MINUTE)
                && today.get(Calendar.HOUR_OF_DAY) <= end.get(Calendar.HOUR_OF_DAY)
                && today.get(Calendar.MINUTE) <= end.get(Calendar.MINUTE)) {
            return true
        }
        return false
    }
}