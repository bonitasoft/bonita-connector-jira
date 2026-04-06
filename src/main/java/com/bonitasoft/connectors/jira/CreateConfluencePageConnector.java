package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates a new Confluence page with content from BPM process.
 * [BETA] - Part of bonita-connector-jira 1.0.0-beta.1
 */
@Slf4j
public class CreateConfluencePageConnector extends AbstractJiraConnector {

    static final String INPUT_SPACE_ID = "spaceId";
    static final String INPUT_PARENT_PAGE_ID = "parentPageId";
    static final String INPUT_PAGE_TITLE = "pageTitle";
    static final String INPUT_PAGE_CONTENT = "pageContent";
    static final String INPUT_CONTENT_FORMAT = "contentFormat";
    static final String INPUT_STATUS = "status";
    static final String INPUT_LABELS = "labels";

    static final String OUTPUT_PAGE_ID = "pageId";
    static final String OUTPUT_PAGE_TITLE = "pageTitle";
    static final String OUTPUT_PAGE_URL = "pageUrl";
    static final String OUTPUT_PAGE_VERSION = "pageVersion";

    @Override
    protected JiraConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .spaceId(readStringInput(INPUT_SPACE_ID))
                .parentPageId(readStringInput(INPUT_PARENT_PAGE_ID))
                .pageTitle(readStringInput(INPUT_PAGE_TITLE))
                .pageContent(readStringInput(INPUT_PAGE_CONTENT))
                .contentFormat(readStringInput(INPUT_CONTENT_FORMAT, "storage"))
                .status(readStringInput(INPUT_STATUS, "current"))
                .labels(readStringInput(INPUT_LABELS))
                .build();
    }

    @Override
    protected void validateConfiguration(JiraConfiguration config) {
        super.validateConfiguration(config);
        if (config.getSpaceId() == null || config.getSpaceId().isBlank()) {
            throw new IllegalArgumentException("spaceId is mandatory");
        }
        if (config.getPageTitle() == null || config.getPageTitle().isBlank()) {
            throw new IllegalArgumentException("pageTitle is mandatory");
        }
        if (config.getPageContent() == null || config.getPageContent().isBlank()) {
            throw new IllegalArgumentException("pageContent is mandatory");
        }
    }

    @Override
    protected void doExecute() throws JiraException {
        log.info("Creating Confluence page: '{}' in space {}", configuration.getPageTitle(), configuration.getSpaceId());

        JiraClient.CreateConfluencePageResult result = client.createConfluencePage(configuration);

        setOutputParameter(OUTPUT_PAGE_ID, result.pageId());
        setOutputParameter(OUTPUT_PAGE_TITLE, result.pageTitle());
        setOutputParameter(OUTPUT_PAGE_URL, result.pageUrl());
        setOutputParameter(OUTPUT_PAGE_VERSION, result.pageVersion());
        setOutputParameter(OUTPUT_ERROR_MESSAGE, "");

        log.info("Created Confluence page: {} ({})", result.pageTitle(), result.pageId());
    }
}
