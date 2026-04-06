package com.bonitasoft.connectors.jira;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CreateIssueConnectorTest {

    @Mock
    private JiraClient mockClient;

    private CreateIssueConnector connector;

    @BeforeEach
    void setUp() {
        connector = new CreateIssueConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("atlassianDomain", "test-company");
        inputs.put("authMode", "BASIC");
        inputs.put("userEmail", "user@test.com");
        inputs.put("apiToken", "test-token-123");
        inputs.put("projectKey", "PROJ");
        inputs.put("summary", "Test issue summary");
        inputs.put("issueType", "Bug");
        inputs.put("connectTimeout", 30000);
        inputs.put("readTimeout", 60000);
        return inputs;
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createIssue(any())).thenReturn(
                new JiraClient.CreateIssueResult("10042", "PROJ-123",
                        "https://test-company.atlassian.net/rest/api/3/issue/10042",
                        "https://test-company.atlassian.net/browse/PROJ-123"));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "issueKey")).isEqualTo("PROJ-123");
        assertThat(getOutput(connector, "issueId")).isEqualTo("10042");
        assertThat(getOutput(connector, "issueUrl")).isEqualTo("https://test-company.atlassian.net/browse/PROJ-123");
        assertThat(getOutput(connector, "errorMessage")).isEqualTo("");
    }

    @Test
    void shouldFailValidationWhenAtlassianDomainMissing() {
        var inputs = validInputs();
        inputs.remove("atlassianDomain");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("atlassianDomain");
    }

    @Test
    void shouldFailValidationWhenProjectKeyMissing() {
        var inputs = validInputs();
        inputs.remove("projectKey");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("projectKey");
    }

    @Test
    void shouldFailValidationWhenSummaryMissing() {
        var inputs = validInputs();
        inputs.remove("summary");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("summary");
    }

    @Test
    void shouldFailValidationWhenBasicAuthMissingEmail() {
        var inputs = validInputs();
        inputs.remove("userEmail");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("userEmail");
    }

    @Test
    void shouldFailValidationWhenBasicAuthMissingApiToken() {
        var inputs = validInputs();
        inputs.remove("apiToken");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("apiToken");
    }

    @Test
    void shouldFailValidationWhenOAuth2MissingClientId() {
        var inputs = validInputs();
        inputs.put("authMode", "OAUTH2");
        inputs.remove("userEmail");
        inputs.remove("apiToken");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("oauthClientId");
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createIssue(any())).thenThrow(
                new JiraException("Issue type 'InvalidType' is not valid for project 'PROJ'", 400, false));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
        assertThat((String) getOutput(connector, "errorMessage"))
                .contains("InvalidType");
    }

    @Test
    void shouldApplyDefaultIssueType() throws Exception {
        var inputs = validInputs();
        inputs.remove("issueType");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        assertThat(connector.configuration.getIssueType()).isEqualTo("Task");
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createIssue(any())).thenReturn(
                new JiraClient.CreateIssueResult("10042", "PROJ-123",
                        "https://test.atlassian.net/rest/api/3/issue/10042",
                        "https://test.atlassian.net/browse/PROJ-123"));

        connector.executeBusinessLogic();

        Map<String, Object> outputs = getOutputs(connector);
        assertThat(outputs.get("issueId")).isNotNull();
        assertThat(outputs.get("issueKey")).isNotNull();
        assertThat(outputs.get("issueSelf")).isNotNull();
        assertThat(outputs.get("issueUrl")).isNotNull();
        assertThat(outputs.get("success")).isNotNull();
        assertThat(outputs.get("errorMessage")).isNotNull();
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractJiraConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOutputs(Object conn) {
        try {
            var method = org.bonitasoft.engine.connector.AbstractConnector.class.getDeclaredMethod("getOutputParameters");
            method.setAccessible(true);
            return (Map<String, Object>) method.invoke(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getOutput(Object conn, String key) {
        return getOutputs(conn).get(key);
    }
}
