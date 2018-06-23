package com.nji.conf

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("filtersConf")
class FiltersConf {

    var filters:HashMap<String, Filter> = hashMapOf()
}
