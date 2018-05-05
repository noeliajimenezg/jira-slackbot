package com.nji.util


import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.core.util.Base64
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import javax.naming.AuthenticationException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.soap.Node
import kotlin.collections.ArrayList
import java.util.*


class JiraUtil {

    companion object {

        val logger = LoggerFactory.getLogger(JiraUtil::class.java)

        /**
         * Connect to JIRA and get the data from the JIRA filter
         * @property username the JIRA username.
         * @property password the JIRA password.
         * @property url the JIRA client url.
         * @property filterId the JIRA filter id.
         * @property elementTagName the XML element tag name that represents an issue in JIRA.
         * @property keyElementTagName the XML element tag name that represents an ID in JIRA.
         * @return a map that contains the ID issue and a map with all the properties of the issue.
         */
        fun getDataFromJira(
                username: String,
                password: String,
                url: String,
                filterId: String,
                elementTagName: String,
                keyElementTagName: String): MutableMap<String, MutableMap<String, String>> {

            val issuesXml: String = getDataFromJiraFilter(username, password, url, filterId)
            return convertXmlToMap(issuesXml, elementTagName, keyElementTagName)
        }

        /**
         * Connect to JIRA and get the data from the JIRA filter already exported to XML
         * @property username the JIRA username.
         * @property password the JIRA password.
         * @property url the JIRA client url.
         * @property filterId the JIRA filter id.
         * @return the data returned by the filter in XML format.
         */
        private fun getDataFromJiraFilter(
                username: String,
                password: String,
                url: String,
                filterId: String): String {

            val auth = String(Base64.encode(username + ":" + password))
            val client = Client.create()
            val webResource = client.resource(url + "/sr/jira.issueviews:searchrequest-xml/" + filterId + "/SearchRequest-" + filterId + ".xml?tempMax=1000")
            val response = webResource.header("Authorization", "Basic $auth")
                    .type("application/json")
                    .accept("application/json")
                    .get(ClientResponse::class.java)

            if (HttpStatus.UNAUTHORIZED.equals(response.status)) {
                throw AuthenticationException("Invalid Username or Password")
            }
            return response.getEntity(String::class.java)
        }

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

        /**
         * Detect the new issues.
         * @property issuesMap the issues that are exported.
         * @property storedIssuesByLine local stored issues that were already treated.
         * @return a list of issues that are detected as new.
         */
        fun detectNewIssues(
                issuesMap: MutableMap<String, MutableMap<String, String>>,
                storedIssuesByLine: MutableList<String>): MutableMap<String, MutableMap<String, String>> {

            val newIssuesMap = mutableMapOf<String, MutableMap<String, String>>()
            for (issueKey in issuesMap.keys) {
                if (!storedIssuesByLine.toString().contains(issueKey)) {
                    newIssuesMap.putIfAbsent(issueKey, issuesMap.get(issueKey)!!)
                }
            }
            logger.info("New anomalies detected: {}", newIssuesMap.size)
            return newIssuesMap
        }


        /**
         * Update the list of local stored issues that are working in progress.
         * @property storedIssuesByPriority the issues that are stored locally.
         * @property issuesMap the issues returned by the Jira filter.
         * @property newIssuesMap the issues detected as new.
         * @property priorities the list of priorities in JIRA.
         * @property priorityName the name of the tag element that represents priority in JIRA.
         * @return a list of issues to be stored order by priority. Each line represents a priority.
         */
        fun updateStoredIssues(
                storedIssuesByPriority: MutableList<String>,
                issuesDataFilterMap: MutableMap<String, MutableMap<String, String>>,
                newIssuesMap: MutableMap<String, MutableMap<String, String>>,
                priorities: List<String>,
                priorityName: String): ArrayList<String> {

            var storedIssuesByLineUpdated = arrayOfNulls<String>(priorities.size)

            var i = 0
            // Store the issues that are still in working progress
            for (storedIssuesLine in storedIssuesByPriority) {
                // The issues are separated by ';'
                val issues: List<String> = storedIssuesLine.split(";")
                val sb = StringBuilder()
                for (issueKey in issues) {
                    // Issues that were already in the file and they're still in working progress
                    if (issuesDataFilterMap.containsKey(issueKey)) {
                        sb.append(issueKey).append(";")
                    }
                }
                storedIssuesByLineUpdated[i] = sb.toString()
                i++
            }
            // Store the issues that are new in their right line (line represents a priority)
            for (newIssue in newIssuesMap) {
                val priority = newIssue.value.get(priorityName)
                val position = priorities.indexOf(priority)
                if (storedIssuesByLineUpdated[position] != null) {
                    storedIssuesByLineUpdated[position] = storedIssuesByLineUpdated[position] + newIssue.key + ";"
                } else {
                    storedIssuesByLineUpdated[position] = newIssue.key + ";"
                }

            }
            // Convert to ArrayList<String>
            return convertToArrayList(storedIssuesByLineUpdated)
        }

        /**
         * Convert the XML data to a map.
         * @property xmlDataJiraFilter the JIRA data in XML format.
         * @property elementTagName the XML element tag name that represents an issue in JIRA.
         * @property keyElementTagName the XML element tag name that represents an ID in JIRA.
         * @return a map that contains the ID issue and a map with all the properties of the issue.
         */
        private fun convertXmlToMap(
                xmlDataJiraFilter: String,
                elementTagName: String,
                keyElementTagName: String): MutableMap<String, MutableMap<String, String>> {

            // Map that contains the ID issue and a map with all the properties of the issue
            val issuesMap = mutableMapOf<String, MutableMap<String, String>>()

            // Parse string to Document
            val xmlDataInputStream = ByteArrayInputStream(xmlDataJiraFilter.toByteArray(Charsets.UTF_8))
            val xmlDataDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlDataInputStream)
            xmlDataDoc.documentElement.normalize()
            // Get the nodes that represent the JIRA issues
            val issues: NodeList = xmlDataDoc.getElementsByTagName(elementTagName)

            // Putting each element of each issue in a map
            for (i in 0..issues.length - 1) {

                var issue: org.w3c.dom.Node? = issues.item(i)

                if (Node.ELEMENT_NODE.equals(issue?.nodeType)) {

                    val elem = issue as Element
                    val issueId = elem.getElementsByTagName(keyElementTagName).item(0).textContent
                    val elementIssuesMap = mutableMapOf<String, String>()
                    val elemChildNodes: NodeList = elem.childNodes
                    for (j in 0..elemChildNodes.length - 1) {
                        elementIssuesMap.putIfAbsent(elemChildNodes.item(j).nodeName, elemChildNodes.item(j).textContent)
                    }
                    // Add to the global map
                    issuesMap.putIfAbsent(issueId, elementIssuesMap)
                }
            }
            return issuesMap
        }

        /**
         * Convert from array to array list.
         * @property array
         * @return an array list.
         */
        private fun convertToArrayList(array: Array<String?>): ArrayList<String> {
            val arrayList: ArrayList<String> = ArrayList<String>()
            for (j in 0..array.size - 1) {
                var line = ""
                if (array[j] != null) {
                    line = array[j]!!
                }
                arrayList.add(line)
            }
            return arrayList
        }
    }
}