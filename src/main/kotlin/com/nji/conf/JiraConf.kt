package com.nji.conf


import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("jira")
class JiraConf {
    lateinit var proxy: String
    lateinit var proxyPort: String
    lateinit var javaHome: String
    lateinit var startTime: String
    lateinit var endTime: String
    var minutes: Int = 15
    lateinit var username: String
    lateinit var password: String
    lateinit var url: String
    lateinit var filterId: String
    lateinit var elementTagName: String
    lateinit var keyElementTagName: String
    lateinit var localFileStoredIssues: String
    lateinit var priorityName: String
    var priority = listOf<String>()
}