package com.nji.conf

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(ignoreUnknownFields = true)
@EnableEncryptableProperties
class BotConf {

    val jira = JiraConf()
    var slack = SlackConf()
}