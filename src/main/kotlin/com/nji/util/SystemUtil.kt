package com.nji.util

import org.slf4j.LoggerFactory
import java.util.*

class SystemUtil {

    companion object {

        val logger = LoggerFactory.getLogger(SystemUtil::class.java)

        /**
         * Set system properties.
         * @property proxy
         * @property proxyPort
         * @property javaHome
         */
        fun setSystemProperties(proxy: String, proxyPort: String, javaHome: String) {
            val sysProperties: Properties = Properties()
            if (!proxy.isBlank()) {
                sysProperties.setProperty("http.proxyHost", proxy)
                sysProperties.setProperty("https.proxyHost", proxy)
            }
            if (!proxyPort.isBlank()) {
                sysProperties.setProperty("http.proxyPort", proxyPort)
                sysProperties.setProperty("https.proxyPort", proxyPort)
            }
            if (!javaHome.isBlank()) {
                sysProperties.setProperty("java.home", javaHome)
            }
            System.setProperties(sysProperties)
        }
    }
}