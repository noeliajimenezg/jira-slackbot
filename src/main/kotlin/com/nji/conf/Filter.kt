package com.nji.conf

class Filter {

    lateinit var filterId: String
    lateinit var slackToken: String
    lateinit var localFileStoredIssues: String
    var slackActive: Boolean = false
    var slackChannels = listOf<String>()
}