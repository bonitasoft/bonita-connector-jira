package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates a new JIRA issue (Bug, Task, Story, Epic, or custom type).
 * [BETA] - Part of bonita-connector-jira 1.0.0-beta.1
 */
@Slf4j
public class CreateIssueConnector extends AbstractJiraConnector {

    static final String INPUT_PROJECT_KEY = "projectKey";
    static final String INPUT_ISSUE_TYPE = "issueType";
    static final String INPUT_SUMMARY = "summary";
    static final String INPUT_DESCRIPTION = "description";
    static final String INPUT_PRIORITY = "priority";
    static final String INPUT_ASSIGNEE_ACCOUNT_ID = "assigneeAccountId";
    static final String INPUT_REPORTER_ACCOUNT_ID = "reporterAccountId";
    static final String INPUT_LABELS = "labels";
    static final String INPUT_COMPONENTS = "components";
    static final String INPUT_DUE_DATE = "dueDate";
    static final String INPUT_PARENT_KEY = "parentKey";
    static final String INPUT_CUSTOM_FIELDS = "customFields";

    static final String OUTPUT_ISSUE_ID = "issueId";
    static final String OUTPUT_ISSUE_KEY = "issueKey";
    static final String OUTPUT_ISSUE_SELF = "issueSelf";
    static final String OUTPUT_ISSUE_URL = "issueUrl";

    @Override
    protected JiraConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .projectKey(readStringInput(INPUT_PROJECT_KEY))
                .issueType(readStringInput(INPUT_ISSUE_TYPE, "Task"))
                .summary(readStringInput(INPUT_SUMMARY))
                .description(readStringInput(INPUT_DESCRIPTION))
                .priority(readStringInput(INPUT_PRIORITY))
                .assigneeAccountId(readStringInput(INPUT_ASSIGNEE_ACCOUNT_ID))
                .reporterAccountId(readStringInput(INPUT_REPORTER_ACCOUNT_ID))
                .labels(readStringInput(INPUT_LABELS))
                .components(readStringInput(INPUT_COMPONENTS))
                .dueDate(readStringInput(INPUT_DUE_DATE))
                .parentKey(readStringInput(INPUT_PARENT_KEY))
                .customFields(readStringInput(INPUT_CUSTOM_FIELDS))
                .build();
    }

    @Override
    protected void validateConfiguration(JiraConfiguration config) {
        super.validateConfiguration(config);
        if (config.getProjectKey() == null || config.getProjectKey().isBlank()) {
            throw new IllegalArgumentException("projectKey is mandatory");
        }
        if (config.getSummary() == null || config.getSummary().isBlank()) {
            throw new IllegalArgumentException("summary is mandatory");
        }
    }

    @Override
    protected void doExecute() throws JiraException {
        log.info("Creating JIRA issue in project {} with type {}",
                configuration.getProjectKey(), configuration.getIssueType());

        JiraClient.CreateIssueResult result = client.createIssue(configuration);

        setOutputParameter(OUTPUT_ISSUE_ID, result.issueId());
        setOutputParameter(OUTPUT_ISSUE_KEY, result.issueKey());
        setOutputParameter(OUTPUT_ISSUE_SELF, result.issueSelf());
        setOutputParameter(OUTPUT_ISSUE_URL, result.issueUrl());
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");

        log.info("Created JIRA issue: {}", result.issueKey());
    }
}
