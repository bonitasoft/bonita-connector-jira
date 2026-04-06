package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;

/**
 * Searches JIRA issues using JQL with pagination support.
 * [BETA] - Part of bonita-connector-jira 1.0.0-beta.1
 */
@Slf4j
public class SearchIssuesConnector extends AbstractJiraConnector {

    static final String INPUT_JQL_QUERY = "jqlQuery";
    static final String INPUT_FIELDS = "fields";
    static final String INPUT_MAX_RESULTS = "maxResults";
    static final String INPUT_START_AT = "startAt";
    static final String INPUT_FETCH_ALL_PAGES = "fetchAllPages";
    static final String INPUT_MAX_TOTAL_RESULTS = "maxTotalResults";

    static final String OUTPUT_ISSUES = "issues";
    static final String OUTPUT_TOTAL_RESULTS = "totalResults";
    static final String OUTPUT_RETURNED_RESULTS = "returnedResults";
    static final String OUTPUT_ISSUE_KEYS = "issueKeys";
    static final String OUTPUT_HAS_MORE_PAGES = "hasMorePages";

    @Override
    protected JiraConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .jqlQuery(readStringInput(INPUT_JQL_QUERY))
                .fields(readStringInput(INPUT_FIELDS, "key,summary,status,priority,assignee"))
                .maxResults(readIntegerInput(INPUT_MAX_RESULTS, 50))
                .startAt(readIntegerInput(INPUT_START_AT, 0))
                .fetchAllPages(readBooleanInput(INPUT_FETCH_ALL_PAGES, false))
                .maxTotalResults(readIntegerInput(INPUT_MAX_TOTAL_RESULTS, 500))
                .build();
    }

    @Override
    protected void validateConfiguration(JiraConfiguration config) {
        super.validateConfiguration(config);
        if (config.getJqlQuery() == null || config.getJqlQuery().isBlank()) {
            throw new IllegalArgumentException("jqlQuery is mandatory");
        }
    }

    @Override
    protected void doExecute() throws JiraException {
        log.info("Searching JIRA issues with JQL: {}", configuration.getJqlQuery());

        JiraClient.SearchIssuesResult result = client.searchIssues(configuration);

        setOutputParameter(OUTPUT_ISSUES, result.issues());
        setOutputParameter(OUTPUT_TOTAL_RESULTS, result.totalResults());
        setOutputParameter(OUTPUT_RETURNED_RESULTS, result.returnedResults());
        setOutputParameter(OUTPUT_ISSUE_KEYS, result.issueKeys());
        setOutputParameter(OUTPUT_HAS_MORE_PAGES, result.hasMorePages());
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");

        log.info("Found {} issues (total: {})", result.returnedResults(), result.totalResults());
    }
}
