package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;

/**
 * Transitions a JIRA issue to a new workflow state.
 * [BETA] - Part of bonita-connector-jira 1.0.0-beta.1
 */
@Slf4j
public class TransitionIssueConnector extends AbstractJiraConnector {

    static final String INPUT_ISSUE_KEY = "issueKey";
    static final String INPUT_TRANSITION_ID = "transitionId";
    static final String INPUT_TRANSITION_NAME = "transitionName";
    static final String INPUT_RESOLUTION = "resolution";
    static final String INPUT_COMMENT = "comment";
    static final String INPUT_TRANSITION_FIELDS = "transitionFields";

    static final String OUTPUT_ISSUE_KEY = "issueKey";
    static final String OUTPUT_NEW_STATUS = "newStatus";
    static final String OUTPUT_TRANSITION_ID = "transitionId";

    @Override
    protected JiraConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .issueKey(readStringInput(INPUT_ISSUE_KEY))
                .transitionId(readStringInput(INPUT_TRANSITION_ID))
                .transitionName(readStringInput(INPUT_TRANSITION_NAME))
                .resolution(readStringInput(INPUT_RESOLUTION))
                .comment(readStringInput(INPUT_COMMENT))
                .transitionFields(readStringInput(INPUT_TRANSITION_FIELDS))
                .build();
    }

    @Override
    protected void validateConfiguration(JiraConfiguration config) {
        super.validateConfiguration(config);
        if (config.getIssueKey() == null || config.getIssueKey().isBlank()) {
            throw new IllegalArgumentException("issueKey is mandatory");
        }
        if ((config.getTransitionId() == null || config.getTransitionId().isBlank())
                && (config.getTransitionName() == null || config.getTransitionName().isBlank())) {
            throw new IllegalArgumentException("Either transitionId or transitionName must be provided");
        }
    }

    @Override
    protected void doExecute() throws JiraException {
        log.info("Transitioning JIRA issue: {} (id={}, name={})",
                configuration.getIssueKey(),
                configuration.getTransitionId(),
                configuration.getTransitionName());

        JiraClient.TransitionIssueResult result = client.transitionIssue(configuration);

        setOutputParameter(OUTPUT_ISSUE_KEY, result.issueKey());
        setOutputParameter(OUTPUT_NEW_STATUS, result.newStatus());
        setOutputParameter(OUTPUT_TRANSITION_ID, result.transitionId());
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");

        log.info("Transitioned JIRA issue {} to status: {}", result.issueKey(), result.newStatus());
    }
}
