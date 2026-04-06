package com.bonitasoft.connectors.jira;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JiraConfigurationTest {

    @Test
    void shouldBuildWithDefaults() {
        var config = JiraConfiguration.builder()
                .atlassianDomain("mycompany")
                .build();

        assertThat(config.getAuthMode()).isEqualTo("BASIC");
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
        assertThat(config.getMaxRetries()).isEqualTo(3);
        assertThat(config.isNotifyUsers()).isTrue();
        assertThat(config.getMaxResults()).isEqualTo(50);
        assertThat(config.getStartAt()).isEqualTo(0);
        assertThat(config.isFetchAllPages()).isFalse();
        assertThat(config.getMaxTotalResults()).isEqualTo(500);
        assertThat(config.getContentFormat()).isEqualTo("storage");
        assertThat(config.getStatus()).isEqualTo("current");
    }

    @Test
    void shouldConstructJiraBaseUrl() {
        var config = JiraConfiguration.builder()
                .atlassianDomain("mycompany")
                .build();
        assertThat(config.jiraBaseUrl()).isEqualTo("https://mycompany.atlassian.net/rest/api/3");
    }

    @Test
    void shouldConstructConfluenceBaseUrl() {
        var config = JiraConfiguration.builder()
                .atlassianDomain("mycompany")
                .build();
        assertThat(config.confluenceBaseUrl()).isEqualTo("https://mycompany.atlassian.net/wiki/api/v2");
    }

    @Test
    void shouldConstructBrowseUrl() {
        var config = JiraConfiguration.builder()
                .atlassianDomain("mycompany")
                .build();
        assertThat(config.browseUrl()).isEqualTo("https://mycompany.atlassian.net/browse/");
    }

    @Test
    void shouldBuildWithAllFields() {
        var config = JiraConfiguration.builder()
                .atlassianDomain("test")
                .authMode("OAUTH2")
                .oauthClientId("client-id")
                .oauthClientSecret("secret")
                .oauthRefreshToken("refresh-token")
                .projectKey("PROJ")
                .issueType("Bug")
                .summary("Test summary")
                .build();

        assertThat(config.getAuthMode()).isEqualTo("OAUTH2");
        assertThat(config.getProjectKey()).isEqualTo("PROJ");
        assertThat(config.getIssueType()).isEqualTo("Bug");
    }
}
