package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;

/**
 * Adds a comment to an existing JIRA issue.
 * [BETA] - Part of bonita-connector-jira 1.0.0-beta.1
 */
@Slf4j
public class AddCommentConnector extends AbstractJiraConnector {

    static final String INPUT_ISSUE_KEY = "issueKey";
    static final String INPUT_COMMENT = "comment";
    static final String INPUT_VISIBILITY = "visibility";

    static final String OUTPUT_COMMENT_ID = "commentId";
    static final String OUTPUT_ISSUE_KEY = "issueKey";
    static final String OUTPUT_CREATED = "created";

    @Override
    protected JiraConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .issueKey(readStringInput(INPUT_ISSUE_KEY))
                .comment(readStringInput(INPUT_COMMENT))
                .visibility(readStringInput(INPUT_VISIBILITY))
                .build();
    }

    @Override
    protected void validateConfiguration(JiraConfiguration config) {
        super.validateConfiguration(config);
        if (config.getIssueKey() == null || config.getIssueKey().isBlank()) {
            throw new IllegalArgumentException("issueKey is mandatory");
        }
        if (config.getComment() == null || config.getComment().isBlank()) {
            throw new IllegalArgumentException("comment is mandatory");
        }
    }

    @Override
    protected void doExecute() throws JiraException {
        log.info("Adding comment to JIRA issue: {}", configuration.getIssueKey());

        JiraClient.AddCommentResult result = client.addComment(configuration);

        setOutputParameter(OUTPUT_COMMENT_ID, result.commentId());
        setOutputParameter(OUTPUT_ISSUE_KEY, result.issueKey());
        setOutputParameter(OUTPUT_CREATED, result.created());
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");

        log.info("Added comment {} to issue {}", result.commentId(), result.issueKey());
    }
}
