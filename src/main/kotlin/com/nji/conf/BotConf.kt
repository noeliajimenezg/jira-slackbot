package com.nji.conf

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(ignoreUnknownFields = true)
@EnableEncryptableProperties
class BotConf{

    lateinit var cronExpression: String
    lateinit var proxy: String
    lateinit var proxyPort: String
    lateinit var javaHome: String
    val jira = JiraConf()
    var slack = SlackConf()
    var filtersConf = FiltersConf()
}