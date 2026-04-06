package com.bonitasoft.connectors.jira;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for JIRA/Confluence connector.
 * Holds connection, auth, and operation parameters.
 */
@Data
@Builder
public class JiraConfiguration {

    // === Connection / Auth parameters (Project/Runtime scope) ===
    private String atlassianDomain;
    @Builder.Default
    private String authMode = "BASIC";
    private String userEmail;
    private String apiToken;
    private String oauthClientId;
    private String oauthClientSecret;
    private String oauthRefreshToken;
    private String cloudId;

    @Builder.Default
    private int connectTimeout = 30000;
    @Builder.Default
    private int readTimeout = 60000;

    @Builder.Default
    private int maxRetries = 3;

    // === Operation parameters (Connector scope) ===
    // Create Issue
    private String projectKey;
    private String issueType;
    private String summary;
    private String description;
    private String priority;
    private String assigneeAccountId;
    private String reporterAccountId;
    private String labels;
    private String components;
    private String dueDate;
    private String parentKey;
    private String customFields;

    // Update Issue
    private String issueKey;
    private String addLabels;
    private String removeLabels;
    @Builder.Default
    private boolean notifyUsers = true;

    // Get Issue
    private String fields;

    // Transition Issue
    private String transitionId;
    private String transitionName;
    private String resolution;
    private String comment;
    private String transitionFields;

    // Search Issues
    private String jqlQuery;
    @Builder.Default
    private int maxResults = 50;
    @Builder.Default
    private int startAt = 0;
    @Builder.Default
    private boolean fetchAllPages = false;
    @Builder.Default
    private int maxTotalResults = 500;

    // Confluence
    private String spaceId;
    private String parentPageId;
    private String pageTitle;
    private String pageContent;
    @Builder.Default
    private String contentFormat = "storage";
    @Builder.Default
    private String status = "current";

    // Comment visibility
    private String visibility;

    public String jiraBaseUrl() {
        return "https://" + atlassianDomain + ".atlassian.net/rest/api/3";
    }

    public String confluenceBaseUrl() {
        return "https://" + atlassianDomain + ".atlassian.net/wiki/api/v2";
    }

    public String browseUrl() {
        return "https://" + atlassianDomain + ".atlassian.net/browse/";
    }

    public String wikiBaseUrl() {
        return "https://" + atlassianDomain + ".atlassian.net/wiki";
    }
}
