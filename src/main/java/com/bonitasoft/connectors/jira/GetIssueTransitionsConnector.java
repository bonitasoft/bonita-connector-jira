package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;

/**
 * Lists available workflow transitions for a JIRA issue.
 * [BETA] - Part of bonita-connector-jira 1.0.0-beta.1
 */
@Slf4j
public class GetIssueTransitionsConnector extends AbstractJiraConnector {

    static final String INPUT_ISSUE_KEY = "issueKey";

    static final String OUTPUT_TRANSITIONS = "transitions";
    static final String OUTPUT_TRANSITION_COUNT = "transitionCount";
    static final String OUTPUT_TRANSITION_NAMES = "transitionNames";
    static final String OUTPUT_TRANSITION_IDS = "transitionIds";

    @Override
    protected JiraConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .issueKey(readStringInput(INPUT_ISSUE_KEY))
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
        log.info("Getting transitions for JIRA issue: {}", configuration.getIssueKey());

        JiraClient.GetIssueTransitionsResult result = client.getIssueTransitions(configuration);

        setOutputParameter(OUTPUT_TRANSITIONS, result.transitions());
        setOutputParameter(OUTPUT_TRANSITION_COUNT, result.transitionCount());
        setOutputParameter(OUTPUT_TRANSITION_NAMES, result.transitionNames());
        setOutputParameter(OUTPUT_TRANSITION_IDS, result.transitionIds());
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");

        log.info("Found {} transitions for issue {}", result.transitionCount(), configuration.getIssueKey());
    }
}
