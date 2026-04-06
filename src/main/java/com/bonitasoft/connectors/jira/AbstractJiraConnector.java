package com.bonitasoft.connectors.jira;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

/**
 * Abstract base connector for JIRA/Confluence operations.
 * Handles connection lifecycle, auth validation, and standard error handling.
 */
@Slf4j
public abstract class AbstractJiraConnector extends AbstractConnector {

    // Output parameter constants
    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected JiraConfiguration configuration;
    protected JiraClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            this.configuration = buildConfiguration();
            validateConfiguration(this.configuration);
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(this, e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new JiraClient(this.configuration);
            log.info("JIRA connector connected successfully");
        } catch (JiraException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (JiraException e) {
            log.error("JIRA connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in JIRA connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, "Unexpected error: " + e.getMessage());
        }
    }

    protected abstract void doExecute() throws JiraException;

    protected abstract JiraConfiguration buildConfiguration();

    protected void validateConfiguration(JiraConfiguration config) {
        if (config.getAtlassianDomain() == null || config.getAtlassianDomain().isBlank()) {
            throw new IllegalArgumentException("atlassianDomain is mandatory");
        }
        String authMode = config.getAuthMode();
        if (authMode == null || "BASIC".equalsIgnoreCase(authMode)) {
            String email = config.getUserEmail();
            String token = config.getApiToken();
            if (email == null || email.isBlank()) {
                // Try env/system property fallback
                email = resolveValue("userEmail", "ATLASSIAN_USER_EMAIL");
            }
            if (token == null || token.isBlank()) {
                token = resolveValue("apiToken", "ATLASSIAN_API_TOKEN");
            }
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("userEmail is required for Basic auth");
            }
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("apiToken is required for Basic auth");
            }
        } else if ("OAUTH2".equalsIgnoreCase(authMode)) {
            if (config.getOauthClientId() == null || config.getOauthClientId().isBlank()) {
                throw new IllegalArgumentException("oauthClientId is required for OAuth2");
            }
            if (config.getOauthClientSecret() == null || config.getOauthClientSecret().isBlank()) {
                throw new IllegalArgumentException("oauthClientSecret is required for OAuth2");
            }
            if (config.getOauthRefreshToken() == null || config.getOauthRefreshToken().isBlank()) {
                throw new IllegalArgumentException("oauthRefreshToken is required for OAuth2");
            }
        }
    }

    private String resolveValue(String key, String envVar) {
        String value = System.getProperty("atlassian." + key);
        if (value == null || value.isBlank()) {
            value = System.getenv(envVar);
        }
        return value;
    }

    protected String readStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    protected String readStringInput(String name, String defaultValue) {
        String value = readStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    protected Integer readIntegerInput(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    /**
     * Builds the base connection configuration common to all operations.
     */
    protected JiraConfiguration.JiraConfigurationBuilder baseConfigBuilder() {
        return JiraConfiguration.builder()
                .atlassianDomain(readStringInput("atlassianDomain"))
                .authMode(readStringInput("authMode", "BASIC"))
                .userEmail(readStringInput("userEmail"))
                .apiToken(readStringInput("apiToken"))
                .oauthClientId(readStringInput("oauthClientId"))
                .oauthClientSecret(readStringInput("oauthClientSecret"))
                .oauthRefreshToken(readStringInput("oauthRefreshToken"))
                .cloudId(readStringInput("cloudId"))
                .connectTimeout(readIntegerInput("connectTimeout", 30000))
                .readTimeout(readIntegerInput("readTimeout", 60000));
    }
}
