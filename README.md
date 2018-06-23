# jira-slackbot
[![Build Status](https://travis-ci.org/noeliajimenezg/jira-slackbot.svg?branch=master)](https://travis-ci.org/noeliajimenezg/jira-slackbot)

# Prerequisites
- JIRA
- Know the ID of a filter for JIRA issues in XML format ([Atlassian](https://confluence.atlassian.com/jira064/displaying-search-results-in-xml-720416695.html))
- Slack application with a token
- Slack channel

# Overview
Spring boot application that runs as a daemon in order to warn on Slack about the new issues created in JIRA.
Here are some of the configurations allowed:
- Frecuency of checking on JIRA is configurable
- Interval during the journey to check on JIRA and warn on Slack
- Proxy connection
- Issues organize by JIRA priority
- Use different Slack channels based on JIRA priority
- Slack message colour can be modified
- New Slack subfields can be added

# Usage
The following provides a description of each property in the configuration file.

- [Jasypt](https://github.com/ulisesbocchio/jasypt-spring-boot) configuration: Jasypt has been added to avoid the exposition of passwords in the configuration file.
`jasypt.encryptor.password`: represents your secret key used in the encryptation. If you need to protect some properties, you could add the encrypted value of the property between 'ENC(encrypted_value)'.  
[Here](http://www.ru-rocker.com/2017/01/13/spring-boot-encrypting-sensitive-variable-properties-file/), you will find a nice example.

- JIRA configuration:  
`proxy`: name of the proxy, in case of need. Example: my.proxy.corp  
`proxy-port`: port used by the proxy. Example: 8080  
`java-home`: location of the JRE installed in the local machine, in case of need.  
`startTime`: time of the day where the check on JIRA and the warn on Slack are sent begin. Example: 08:00  
`endTime`: time of the day where the check on JIRA and the warn on Slack are sent end. Example: 18:00  
`minutes`: frecuency of check on JIRA in minutes. Example: 15  
`username`: username to login in JIRA. Example of property encrypted: ENC(42gdc36jk8903)  
`password`: password to login in JIRA. Example of property encrypted: ENC(s,qpiejidiw3)  
`url`: JIRA website. Example: https://jira.atlassian.com  
`filter-id`: ID of the filter from JIRA. It is showed on JIRA url. Example: 16729  
`element-tag-name`: JIRA XML tag name for the element that represents the JIRA issues. Example: item  
`key-element-tag-name`: JIRA XML tag name for the element that represents the ID of the JIRA issue. Example: key  
`local-file-stored-issues`: local file that contains the issues already treated. Example: C:\issues.txt  
`priority-name`: JIRA XML tag name for the element that represents the priority of the JIRA issues. Example: priority  
`priority`: list of priority's name in your organisation. Example: High, Medium, Low  

- Slack configuration:  
`active`: flag to activate/deactivate the warnings on Slack  
`token`: token of the Slack application that represents the bot. [Official documentation](https://api.slack.com/slack-apps) to create an Slack application.  
`channels`: list of Slack channels where to send the warnings depending on issues priority.  
`colors`: colors of Slack messages depending on issues priority.  
`fields.slack-message.title`: map between the slack message title and the name of the field in JIRA that will be showed.  
`fields.slack-message.title-link`: map between the slack message link and the link of the JIRA issue.  
`subfields`: map between the differents subfields that could be showed on Slack message and the field of the JIRA issue. New subfields can be added.  