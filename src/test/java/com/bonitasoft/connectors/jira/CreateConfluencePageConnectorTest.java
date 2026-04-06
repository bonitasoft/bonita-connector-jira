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
class CreateConfluencePageConnectorTest {

    @Mock
    private JiraClient mockClient;

    private CreateConfluencePageConnector connector;

    @BeforeEach
    void setUp() {
        connector = new CreateConfluencePageConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("atlassianDomain", "test-company");
        inputs.put("userEmail", "user@test.com");
        inputs.put("apiToken", "test-token");
        inputs.put("spaceId", "12345");
        inputs.put("pageTitle", "Test Page");
        inputs.put("pageContent", "<p>Hello World</p>");
        return inputs;
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createConfluencePage(any())).thenReturn(
                new JiraClient.CreateConfluencePageResult("67890", "Test Page",
                        "https://test.atlassian.net/wiki/spaces/TEST/pages/67890", 1));

        connector.executeBusinessLogic();

        assertThat(getOutput(connector, "success")).isEqualTo(true);
        assertThat(getOutput(connector, "pageId")).isEqualTo("67890");
    }

    @Test
    void shouldFailValidationWhenSpaceIdMissing() {
        var inputs = validInputs();
        inputs.remove("spaceId");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("spaceId");
    }

    @Test
    void shouldFailValidationWhenPageTitleMissing() {
        var inputs = validInputs();
        inputs.remove("pageTitle");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("pageTitle");
    }

    @Test
    void shouldFailValidationWhenPageContentMissing() {
        var inputs = validInputs();
        inputs.remove("pageContent");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("pageContent");
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
