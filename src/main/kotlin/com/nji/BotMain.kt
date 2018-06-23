package com.nji


import com.nji.conf.BotConf
import com.nji.conf.Filter
import com.nji.util.FileUtil.Companion.readStoredIssues
import com.nji.util.FileUtil.Companion.saveIssuesToFile
import com.nji.util.JiraUtil.Companion.detectNewIssues
import com.nji.util.JiraUtil.Companion.getDataFromJira
import com.nji.util.JiraUtil.Companion.updateStoredIssues
import com.nji.util.SlackUtil.Companion.sendIssuesToSlack
import com.nji.util.SystemUtil.Companion.setSystemProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


@Component
class BotMain : CommandLineRunner {

    val logger = LoggerFactory.getLogger(BotMain::class.java)

    @Autowired
    lateinit var botConf: BotConf

    val MINUTE_CHAR_POSITION = 4

    override fun run(vararg args: String?) {
        logger.info("Let's check the issues every {} minutes", botConf.cronExpression.get(MINUTE_CHAR_POSITION))
    }

    @Scheduled(cron = "\${cronExpression}")
    private fun runBot(){
        setSystemProperties(botConf.proxy, botConf.proxyPort, botConf.javaHome)
        botConf.filtersConf.filters.forEach { team, filter -> checkFilter(team, filter)  }
        logger.info("Ending check...")
    }

    /**
     * Check a JIRA filter and send Slack messages if new issues were found
     * @property filterKey the name of the key used for the filter in the configuration
     * @property filter the filter's configuration
     */
    private fun checkFilter(filterKey: String, filter: Filter){
        logger.info("Checking filter: {}", filterKey)

        // Connection and getting the data from JIRA filter
        val issuesFilter = getDataFromJira(
                botConf.jira.username,
                botConf.jira.password,
                botConf.jira.url,
                botConf.jira.elementTagName,
                botConf.jira.keyElementTagName,
                filter.filterId)

        // Read local stored issues. Each line represents a priority
        val storedIssues = readStoredIssues(filter.localFileStoredIssues)

        // Get the new Jira issues to be sent to Slack
        val newIssues: MutableMap<String, MutableMap<String, String>> = detectNewIssues(issuesFilter, storedIssues)

        // Update the issues in the file
        val storedIssuesUpdated: List<String> = updateStoredIssues(
                storedIssues,
                issuesFilter,
                newIssues,
                botConf.jira.priority,
                botConf.jira.priorityName)

        // Save to file
        saveIssuesToFile(storedIssuesUpdated, filter.localFileStoredIssues)

        // Send to Slack
        if (isSlackActiveGlobal() && isSlackActiveForFilter(filter) && !newIssues.isEmpty()) {
            sendIssuesToSlack(
                    filter.slackToken,
                    filter.slackChannels,
                    botConf.slack.colors,
                    botConf.slack.fields,
                    botConf.slack.subfields,
                    botConf.jira.priorityName,
                    botConf.jira.priority,
                    newIssues)
        }
    }

    /**
     * Check if Slack is activated globally
     * @return Boolean
     */
    private fun isSlackActiveGlobal(): Boolean{
        return botConf.slack.active;
    }

    /**
     * Check if Slack is activated for the filter
     * @property filter the filter's configuration
     * @return Boolean
     */
    private fun isSlackActiveForFilter(filter : Filter): Boolean{
        return filter.slackActive;
    }
}



