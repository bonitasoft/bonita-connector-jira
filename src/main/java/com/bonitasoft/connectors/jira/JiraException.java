package com.bonitasoft.connectors.jira;

/**
 * Typed exception for JIRA/Confluence connector operations.
 */
public class JiraException extends Exception {

    private final int statusCode;
    private final boolean retryable;

    public JiraException(String message) {
        super(message);
        this.statusCode = -1;
        this.retryable = false;
    }

    public JiraException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = false;
    }

    public JiraException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public JiraException(String message, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
