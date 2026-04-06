package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;

/**
 * Retrieves full details of a JIRA issue.
 * [BETA] - Part of bonita-connector-jira 1.0.0-beta.1
 */
@Slf4j
public class GetIssueConnector extends AbstractJiraConnector {

    static final String INPUT_ISSUE_KEY = "issueKey";
    static final String INPUT_FIELDS = "fields";

    static final String OUTPUT_ISSUE_KEY = "issueKey";
    static final String OUTPUT_ISSUE_ID = "issueId";
    static final String OUTPUT_SUMMARY = "summary";
    static final String OUTPUT_DESCRIPTION = "description";
    static final String OUTPUT_STATUS_NAME = "statusName";
    static final String OUTPUT_STATUS_CATEGORY = "statusCategory";
    static final String OUTPUT_PRIORITY_NAME = "priorityName";
    static final String OUTPUT_ASSIGNEE_DISPLAY_NAME = "assigneeDisplayName";
    static final String OUTPUT_ASSIGNEE_ACCOUNT_ID = "assigneeAccountId";
    static final String OUTPUT_REPORTER_DISPLAY_NAME = "reporterDisplayName";
    static final String OUTPUT_ISSUE_TYPE_NAME = "issueTypeName";
    static final String OUTPUT_PROJECT_KEY = "projectKey";
    static final String OUTPUT_LABELS = "labels";
    static final String OUTPUT_CREATED = "created";
    static final String OUTPUT_UPDATED = "updated";
    static final String OUTPUT_DUE_DATE = "dueDate";
    static final String OUTPUT_RESOLUTION = "resolution";
    static final String OUTPUT_COMMENT_COUNT = "commentCount";
    static final String OUTPUT_ISSUE_JSON = "issueJson";
    static final String OUTPUT_ISSUE_URL = "issueUrl";

    @Override
    protected JiraConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .issueKey(readStringInput(INPUT_ISSUE_KEY))
                .fields(readStringInput(INPUT_FIELDS, "*all"))
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
        log.info("Getting JIRA issue: {}", configuration.getIssueKey());

        JiraClient.GetIssueResult result = client.getIssue(configuration);

        setOutputParameter(OUTPUT_ISSUE_KEY, result.issueKey());
        setOutputParameter(OUTPUT_ISSUE_ID, result.issueId());
        setOutputParameter(OUTPUT_SUMMARY, result.summary());
        setOutputParameter(OUTPUT_DESCRIPTION, result.description());
        setOutputParameter(OUTPUT_STATUS_NAME, result.statusName());
        setOutputParameter(OUTPUT_STATUS_CATEGORY, result.statusCategory());
        setOutputParameter(OUTPUT_PRIORITY_NAME, result.priorityName());
        setOutputParameter(OUTPUT_ASSIGNEE_DISPLAY_NAME, result.assigneeDisplayName());
        setOutputParameter(OUTPUT_ASSIGNEE_ACCOUNT_ID, result.assigneeAccountId());
        setOutputParameter(OUTPUT_REPORTER_DISPLAY_NAME, result.reporterDisplayName());
        setOutputParameter(OUTPUT_ISSUE_TYPE_NAME, result.issueTypeName());
        setOutputParameter(OUTPUT_PROJECT_KEY, result.projectKey());
        setOutputParameter(OUTPUT_LABELS, result.labels());
        setOutputParameter(OUTPUT_CREATED, result.created());
        setOutputParameter(OUTPUT_UPDATED, result.updated());
        setOutputParameter(OUTPUT_DUE_DATE, result.dueDate());
        setOutputParameter(OUTPUT_RESOLUTION, result.resolution());
        setOutputParameter(OUTPUT_COMMENT_COUNT, result.commentCount());
        setOutputParameter(OUTPUT_ISSUE_JSON, result.issueJson());
        setOutputParameter(OUTPUT_ISSUE_URL, result.issueUrl());
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");

        log.info("Retrieved JIRA issue: {} (status: {})", result.issueKey(), result.statusName());
    }
}
