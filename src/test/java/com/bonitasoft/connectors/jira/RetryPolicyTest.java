package com.bonitasoft.connectors.jira;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void shouldReturnResultOnFirstAttempt() throws JiraException {
        var policy = new RetryPolicy(3);
        String result = policy.execute(() -> "success");
        assertThat(result).isEqualTo("success");
    }

    @Test
    void shouldRetryOnRetryableException() throws JiraException {
        var policy = new RetryPolicy(3) {
            @Override
            void sleep(long millis) {
                // no-op for tests
            }
        };
        var counter = new int[]{0};
        String result = policy.execute(() -> {
            counter[0]++;
            if (counter[0] < 3) {
                throw new JiraException("Rate limited", 429, true);
            }
            return "success";
        });
        assertThat(result).isEqualTo("success");
        assertThat(counter[0]).isEqualTo(3);
    }

    @Test
    void shouldFailImmediatelyOnNonRetryableException() {
        var policy = new RetryPolicy(3);
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new JiraException("Unauthorized", 401, false);
        })).isInstanceOf(JiraException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void shouldFailAfterMaxRetries() {
        var policy = new RetryPolicy(2) {
            @Override
            void sleep(long millis) {
                // no-op
            }
        };
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new JiraException("Server error", 500, true);
        })).isInstanceOf(JiraException.class)
                .hasMessageContaining("Server error");
    }

    @Test
    void shouldCalculateExponentialWait() {
        var policy = new RetryPolicy(3);
        long wait0 = policy.calculateWait(0);
        long wait1 = policy.calculateWait(1);
        long wait2 = policy.calculateWait(2);
        // Exponential growth with jitter
        assertThat(wait0).isGreaterThanOrEqualTo(1000L);
        assertThat(wait1).isGreaterThanOrEqualTo(2000L);
        assertThat(wait2).isGreaterThanOrEqualTo(4000L);
    }

    @Test
    void shouldIdentifyRetryableStatusCodes() {
        assertThat(RetryPolicy.isRetryableStatusCode(429)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(500)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(502)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(503)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(400)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(401)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(404)).isFalse();
    }

    @Test
    void shouldWrapUnexpectedExceptions() {
        var policy = new RetryPolicy(3);
        assertThatThrownBy(() -> policy.execute(() -> {
            throw new RuntimeException("unexpected");
        })).isInstanceOf(JiraException.class)
                .hasMessageContaining("Unexpected error");
    }
}
