package com.nji.conf

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(ignoreUnknownFields = true)
class BotConf{

    val jira = JiraConf()
    var slack = SlackConf()
}