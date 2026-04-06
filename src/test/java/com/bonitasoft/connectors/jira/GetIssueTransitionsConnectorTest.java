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
class GetIssueTransitionsConnectorTest {

    @Mock
    private JiraClient mockClient;

    private GetIssueTransitionsConnector connector;

    @BeforeEach
    void setUp() {
        connector = new GetIssueTransitionsConnector();
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

        when(mockClient.getIssueTransitions(any())).thenReturn(
                new JiraClient.GetIssueTransitionsResult("[]", 0, "", ""));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "transitionCount")).isEqualTo(0);
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
