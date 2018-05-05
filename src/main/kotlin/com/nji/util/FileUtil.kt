package com.nji.util

import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream

class FileUtil {

    companion object {

        val logger = LoggerFactory.getLogger(FileUtil::class.java)

        /**
         * Read from local file the JIRA issues that were already stored.
         * @property localFileStoredIssues path to the local file.
         * @return a list of strings with the ID issues concatenated by ';'.
         */
        fun readStoredIssues(localFileStoredIssues: String): MutableList<String> {

            val localStoredIssues: InputStream = File(localFileStoredIssues).inputStream()
            val localStoredIssuesLineList = mutableListOf<String>()
            localStoredIssues.bufferedReader().useLines { lines -> lines.forEach { localStoredIssuesLineList.add(it) } }
            return localStoredIssuesLineList
        }

        /**
         * Save issues to file.
         * @property storedIssuesByLineUpdated the list of issues ordered by priority.
         * @property localFileStoredIssues path to the file.
         */
        fun saveIssuesToFile(storedIssuesByLineUpdated: ArrayList<String>, localFileStoredIssues: String) {
            File(localFileStoredIssues).bufferedWriter().use { out ->
                storedIssuesByLineUpdated.forEach {
                    out.append(it + System.lineSeparator())
                }
            }
            logger.info("File was saved")
        }
    }
}