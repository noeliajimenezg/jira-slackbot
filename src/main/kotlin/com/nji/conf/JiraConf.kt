package com.nji.conf

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


@ConfigurationProperties("jira")
class JiraConf {
    lateinit var username: String
    lateinit var password: String
    lateinit var url: String
    lateinit var elementTagName: String
    lateinit var keyElementTagName: String
    lateinit var priorityName: String
    var priority = listOf<String>()
}