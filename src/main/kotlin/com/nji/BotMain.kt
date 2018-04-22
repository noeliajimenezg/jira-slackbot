package com.nji


import com.nji.conf.BotConf
import com.nji.conf.JiraConf
import com.nji.conf.SlackConf
import com.nji.util.FileUtil.Companion.readStoredIssues
import com.nji.util.FileUtil.Companion.saveIssuesToFile
import com.nji.util.JiraUtil.Companion.convertXmlToMap
import com.nji.util.JiraUtil.Companion.detectNewIssues
import com.nji.util.JiraUtil.Companion.getDataFromJiraFilter
import com.nji.util.JiraUtil.Companion.updateLocalStoredIssues
import com.nji.util.SlackUtil.Companion.sendIssuesToSlack
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.util.*
import kotlin.concurrent.timerTask


@Component
class BotMain : CommandLineRunner {

    val logger = LoggerFactory.getLogger(BotMain::class.java)

    @Autowired
    lateinit var botConf: BotConf

    override fun run(vararg args: String?) {

        // Configuration for JIRA and Slack
        val jira: JiraConf = botConf.jira
        val slack: SlackConf = botConf.slack
        val seconds: Long = (jira.minutes * 60 * 1000).toLong()
        logger.info("Let's check the issues every {} minutes", jira.minutes)
        checkIssues(jira, slack)
        //val timer = Timer()
        //timer.schedule(timerTask { checkIssues(jira, slack) }, seconds)

    }


    private fun checkIssues(jira: JiraConf, slack: SlackConf){

        // Connection and getting the data from JIRA filter
        val xmlDataFilter: String = getDataFromJiraFilter(jira.username, jira.password, jira.url, jira.filterId)

        // Convert the data from XML to Map
        val issuesDataFilterMap = convertXmlToMap(xmlDataFilter, jira.elementTagName, jira.keyElementTagName)

        // Read local stored issues. Each line represents a priority
        val storedIssuesByPriority = readStoredIssues(jira.localFileStoredIssues)

        // Get the new Jira issues to be sent to Slack
        val newIssuesMap: MutableMap<String, MutableMap<String, String>> = detectNewIssues(issuesDataFilterMap, storedIssuesByPriority)

        // Update the issues in the file
        val storedIssuesByLineUpdated : ArrayList<String> = updateLocalStoredIssues(storedIssuesByPriority, issuesDataFilterMap, newIssuesMap, jira.priority, jira.priorityName)

        // Save to file
        saveIssuesToFile(storedIssuesByLineUpdated, jira.localFileStoredIssues)

        // Send to Slack
        if (slack.active) {
            sendIssuesToSlack(slack.token, slack.channels, slack.colors, slack.fields, slack.subfields, jira.priorityName, jira.priority, newIssuesMap)
        }
    }
}



