package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;

/**
 * Updates fields of an existing JIRA issue.
 * [BETA] - Part of bonita-connector-jira 1.0.0-beta.1
 */
@Slf4j
public class UpdateIssueConnector extends AbstractJiraConnector {

    static final String INPUT_ISSUE_KEY = "issueKey";
    static final String INPUT_SUMMARY = "summary";
    static final String INPUT_DESCRIPTION = "description";
    static final String INPUT_PRIORITY = "priority";
    static final String INPUT_ASSIGNEE_ACCOUNT_ID = "assigneeAccountId";
    static final String INPUT_LABELS = "labels";
    static final String INPUT_ADD_LABELS = "addLabels";
    static final String INPUT_REMOVE_LABELS = "removeLabels";
    static final String INPUT_DUE_DATE = "dueDate";
    static final String INPUT_CUSTOM_FIELDS = "customFields";
    static final String INPUT_NOTIFY_USERS = "notifyUsers";

    static final String OUTPUT_ISSUE_KEY = "issueKey";

    @Override
    protected JiraConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .issueKey(readStringInput(INPUT_ISSUE_KEY))
                .summary(readStringInput(INPUT_SUMMARY))
                .description(readStringInput(INPUT_DESCRIPTION))
                .priority(readStringInput(INPUT_PRIORITY))
                .assigneeAccountId(readStringInput(INPUT_ASSIGNEE_ACCOUNT_ID))
                .labels(readStringInput(INPUT_LABELS))
                .addLabels(readStringInput(INPUT_ADD_LABELS))
                .removeLabels(readStringInput(INPUT_REMOVE_LABELS))
                .dueDate(readStringInput(INPUT_DUE_DATE))
                .customFields(readStringInput(INPUT_CUSTOM_FIELDS))
                .notifyUsers(readBooleanInput(INPUT_NOTIFY_USERS, true))
                .build();
    }

    @Override
    protected void validateConfiguration(JiraConfiguration config) {
        super.validateConfiguration(config);
        if (config.getIssueKey() == null || config.getIssueKey().isBlank()) {
            throw new IllegalArgumentException("issueKey is mandatory");
        }
    }

    @Override
    protected void doExecute() throws JiraException {
        log.info("Updating JIRA issue: {}", configuration.getIssueKey());

        JiraClient.UpdateIssueResult result = client.updateIssue(configuration);

        setOutputParameter(OUTPUT_ISSUE_KEY, result.issueKey());
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");

        log.info("Updated JIRA issue: {}", result.issueKey());
    }
}
