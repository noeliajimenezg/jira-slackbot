package com.nji.conf

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("slack")
class SlackConf {
    var active: Boolean = false
    var colors = listOf<String>()
    var fields: HashMap<String, String> = hashMapOf()
    var subfields: HashMap<String, SlackSubfield> = hashMapOf()
}