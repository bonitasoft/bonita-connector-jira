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
class GetIssueConnectorTest {

    @Mock
    private JiraClient mockClient;

    private GetIssueConnector connector;

    @BeforeEach
    void setUp() {
        connector = new GetIssueConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("atlassianDomain", "test-company");
        inputs.put("userEmail", "user@test.com");
        inputs.put("apiToken", "test-token");
        inputs.put("issueKey", "PROJ-123");
        return inputs;
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getIssue(any())).thenReturn(
                new JiraClient.GetIssueResult(
                        "PROJ-123", "10042", "Test summary", "Test description",
                        "In Progress", "indeterminate", "High",
                        "John Doe", "acc123", "Jane Smith",
                        "Bug", "PROJ", "bonita,automated",
                        "2026-03-25T10:00:00.000+0000", "2026-03-25T12:00:00.000+0000",
                        "2026-04-15", "", 5, "{}", "https://test.atlassian.net/browse/PROJ-123"));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "issueKey")).isEqualTo("PROJ-123");
        assertThat(getOutput(connector, "statusName")).isEqualTo("In Progress");
        assertThat(getOutput(connector, "commentCount")).isEqualTo(5);
    }

    @Test
    void shouldFailValidationWhenIssueKeyMissing() {
        var inputs = validInputs();
        inputs.remove("issueKey");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("issueKey");
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getIssue(any())).thenThrow(new JiraException("Not found: PROJ-999", 404, false));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(false);
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
