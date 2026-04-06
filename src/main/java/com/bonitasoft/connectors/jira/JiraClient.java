package com.bonitasoft.connectors.jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * HTTP client facade for JIRA REST API v3 and Confluence REST API v2.
 * Uses java.net.http.HttpClient with Basic or OAuth2 authentication.
 */
@Slf4j
public class JiraClient {

    private final JiraConfiguration configuration;
    private final RetryPolicy retryPolicy;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // OAuth2 state
    private String accessToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public JiraClient(JiraConfiguration configuration) throws JiraException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configuration.getConnectTimeout()))
                .build();

        if ("OAUTH2".equalsIgnoreCase(configuration.getAuthMode())) {
            refreshOAuth2Token();
        }
        log.debug("JiraClient initialized with auth mode: {}", configuration.getAuthMode());
    }

    // ========== Operations ==========

    public CreateIssueResult createIssue(JiraConfiguration config) throws JiraException {
        return retryPolicy.execute(() -> {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode fields = body.putObject("fields");
            fields.putObject("project").put("key", config.getProjectKey());
            fields.putObject("issuetype").put("name", config.getIssueType() != null ? config.getIssueType() : "Task");
            fields.put("summary", config.getSummary());

            if (config.getDescription() != null && !config.getDescription().isBlank()) {
                fields.set("description", markdownToAdf(config.getDescription()));
            }
            if (config.getPriority() != null && !config.getPriority().isBlank()) {
                fields.putObject("priority").put("name", config.getPriority());
            }
            if (config.getAssigneeAccountId() != null && !config.getAssigneeAccountId().isBlank()) {
                fields.putObject("assignee").put("accountId", config.getAssigneeAccountId());
            }
            if (config.getReporterAccountId() != null && !config.getReporterAccountId().isBlank()) {
                fields.putObject("reporter").put("accountId", config.getReporterAccountId());
            }
            if (config.getLabels() != null && !config.getLabels().isBlank()) {
                ArrayNode labelsArr = fields.putArray("labels");
                Arrays.stream(config.getLabels().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .forEach(labelsArr::add);
            }
            if (config.getComponents() != null && !config.getComponents().isBlank()) {
                ArrayNode compsArr = fields.putArray("components");
                Arrays.stream(config.getComponents().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .forEach(c -> compsArr.addObject().put("name", c));
            }
            if (config.getDueDate() != null && !config.getDueDate().isBlank()) {
                fields.put("duedate", config.getDueDate());
            }
            if (config.getParentKey() != null && !config.getParentKey().isBlank()) {
                fields.putObject("parent").put("key", config.getParentKey());
            }
            if (config.getCustomFields() != null && !config.getCustomFields().isBlank()) {
                try {
                    JsonNode custom = objectMapper.readTree(config.getCustomFields());
                    custom.fields().forEachRemaining(entry -> fields.set(entry.getKey(), entry.getValue()));
                } catch (JsonProcessingException e) {
                    throw new JiraException("Invalid customFields JSON: " + e.getMessage());
                }
            }

            String url = config.jiraBaseUrl() + "/issue";
            HttpResponse<String> response = sendRequest("POST", url, body.toString());
            JsonNode responseJson = parseResponse(response, 201);

            String issueId = responseJson.path("id").asText();
            String issueKey = responseJson.path("key").asText();
            String issueSelf = responseJson.path("self").asText();
            String issueUrl = config.browseUrl() + issueKey;
            return new CreateIssueResult(issueId, issueKey, issueSelf, issueUrl);
        });
    }

    public UpdateIssueResult updateIssue(JiraConfiguration config) throws JiraException {
        return retryPolicy.execute(() -> {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode fields = body.putObject("fields");
            ObjectNode update = body.putObject("update");
            boolean hasFields = false;

            if (config.getSummary() != null && !config.getSummary().isBlank()) {
                fields.put("summary", config.getSummary());
                hasFields = true;
            }
            if (config.getDescription() != null && !config.getDescription().isBlank()) {
                fields.set("description", markdownToAdf(config.getDescription()));
                hasFields = true;
            }
            if (config.getPriority() != null && !config.getPriority().isBlank()) {
                fields.putObject("priority").put("name", config.getPriority());
                hasFields = true;
            }
            if (config.getAssigneeAccountId() != null && !config.getAssigneeAccountId().isBlank()) {
                if ("-1".equals(config.getAssigneeAccountId())) {
                    fields.putNull("assignee");
                } else {
                    fields.putObject("assignee").put("accountId", config.getAssigneeAccountId());
                }
                hasFields = true;
            }
            if (config.getDueDate() != null && !config.getDueDate().isBlank()) {
                fields.put("duedate", config.getDueDate());
                hasFields = true;
            }
            if (config.getCustomFields() != null && !config.getCustomFields().isBlank()) {
                try {
                    JsonNode custom = objectMapper.readTree(config.getCustomFields());
                    custom.fields().forEachRemaining(entry -> fields.set(entry.getKey(), entry.getValue()));
                    hasFields = true;
                } catch (JsonProcessingException e) {
                    throw new JiraException("Invalid customFields JSON: " + e.getMessage());
                }
            }

            // Label operations via update syntax
            if (config.getLabels() != null && !config.getLabels().isBlank()) {
                ArrayNode labelOps = update.putArray("labels");
                // Replace all: first remove existing (JIRA does replace via set)
                ArrayNode labelsArr = fields.putArray("labels");
                Arrays.stream(config.getLabels().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .forEach(labelsArr::add);
                hasFields = true;
            } else {
                if (config.getAddLabels() != null && !config.getAddLabels().isBlank()) {
                    ArrayNode labelOps = update.putArray("labels");
                    Arrays.stream(config.getAddLabels().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty())
                            .forEach(l -> labelOps.addObject().put("add", l));
                    hasFields = true;
                }
                if (config.getRemoveLabels() != null && !config.getRemoveLabels().isBlank()) {
                    ArrayNode labelOps;
                    if (update.has("labels")) {
                        labelOps = (ArrayNode) update.get("labels");
                    } else {
                        labelOps = update.putArray("labels");
                    }
                    Arrays.stream(config.getRemoveLabels().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty())
                            .forEach(l -> labelOps.addObject().put("remove", l));
                    hasFields = true;
                }
            }

            if (!hasFields && !update.fields().hasNext()) {
                return new UpdateIssueResult(config.getIssueKey());
            }

            // Clean up empty nodes
            if (!fields.fields().hasNext()) {
                body.remove("fields");
            }
            if (!update.fields().hasNext()) {
                body.remove("update");
            }

            String url = config.jiraBaseUrl() + "/issue/" + config.getIssueKey();
            if (!config.isNotifyUsers()) {
                url += "?notifyUsers=false";
            }
            HttpResponse<String> response = sendRequest("PUT", url, body.toString());
            if (response.statusCode() == 204 || response.statusCode() == 200) {
                return new UpdateIssueResult(config.getIssueKey());
            }
            parseResponse(response, 204);
            return new UpdateIssueResult(config.getIssueKey());
        });
    }

    public GetIssueResult getIssue(JiraConfiguration config) throws JiraException {
        return retryPolicy.execute(() -> {
            StringBuilder url = new StringBuilder(config.jiraBaseUrl())
                    .append("/issue/").append(config.getIssueKey());

            String fieldsParam = config.getFields();
            if (fieldsParam != null && !fieldsParam.isBlank() && !"*all".equals(fieldsParam)) {
                url.append("?fields=").append(fieldsParam);
            }

            HttpResponse<String> response = sendRequest("GET", url.toString(), null);
            JsonNode json = parseResponse(response, 200);

            JsonNode fieldsNode = json.path("fields");
            String issueKey = json.path("key").asText();
            String issueId = json.path("id").asText();
            String summary = fieldsNode.path("summary").asText("");
            String description = adfToPlainText(fieldsNode.path("description"));
            String statusName = fieldsNode.path("status").path("name").asText("");
            String statusCategory = fieldsNode.path("status").path("statusCategory").path("key").asText("");
            String priorityName = fieldsNode.path("priority").path("name").asText("");
            String assigneeDisplayName = fieldsNode.path("assignee").path("displayName").asText("");
            String assigneeAccountId = fieldsNode.path("assignee").path("accountId").asText("");
            String reporterDisplayName = fieldsNode.path("reporter").path("displayName").asText("");
            String issueTypeName = fieldsNode.path("issuetype").path("name").asText("");
            String projectKey = fieldsNode.path("project").path("key").asText("");

            ArrayNode labelsArr = (ArrayNode) fieldsNode.path("labels");
            String labels = labelsArr.isMissingNode() ? "" :
                    StreamSupport.stream(labelsArr.spliterator(), false)
                            .map(JsonNode::asText).collect(Collectors.joining(","));

            String created = fieldsNode.path("created").asText("");
            String updated = fieldsNode.path("updated").asText("");
            String dueDateVal = fieldsNode.path("duedate").asText("");
            String resolutionVal = fieldsNode.path("resolution").path("name").asText("");
            int commentCount = fieldsNode.path("comment").path("total").asInt(0);
            String issueJson = json.toString();
            String issueUrl = config.browseUrl() + issueKey;

            return new GetIssueResult(issueKey, issueId, summary, description,
                    statusName, statusCategory, priorityName,
                    assigneeDisplayName, assigneeAccountId, reporterDisplayName,
                    issueTypeName, projectKey, labels, created, updated,
                    dueDateVal, resolutionVal, commentCount, issueJson, issueUrl);
        });
    }

    public TransitionIssueResult transitionIssue(JiraConfiguration config) throws JiraException {
        return retryPolicy.execute(() -> {
            String actualTransitionId = config.getTransitionId();

            // Resolve transition by name if ID not provided
            if ((actualTransitionId == null || actualTransitionId.isBlank())
                    && config.getTransitionName() != null && !config.getTransitionName().isBlank()) {
                GetIssueTransitionsResult transitions = getIssueTransitions(config);
                actualTransitionId = findTransitionIdByName(transitions, config.getTransitionName());
            }

            if (actualTransitionId == null || actualTransitionId.isBlank()) {
                throw new JiraException("Either transitionId or transitionName must be provided");
            }

            ObjectNode body = objectMapper.createObjectNode();
            body.putObject("transition").put("id", actualTransitionId);

            // Add resolution if provided
            if (config.getResolution() != null && !config.getResolution().isBlank()) {
                if (!body.has("fields")) {
                    body.putObject("fields");
                }
                ((ObjectNode) body.get("fields"))
                        .putObject("resolution").put("name", config.getResolution());
            }

            // Add comment if provided
            if (config.getComment() != null && !config.getComment().isBlank()) {
                ObjectNode updateNode = body.putObject("update");
                ArrayNode commentOps = updateNode.putArray("comment");
                ObjectNode addOp = commentOps.addObject();
                addOp.set("add", objectMapper.createObjectNode()
                        .set("body", markdownToAdf(config.getComment())));
            }

            // Add transition fields if provided
            if (config.getTransitionFields() != null && !config.getTransitionFields().isBlank()) {
                try {
                    JsonNode transFields = objectMapper.readTree(config.getTransitionFields());
                    if (!body.has("fields")) {
                        body.putObject("fields");
                    }
                    transFields.fields().forEachRemaining(entry ->
                            ((ObjectNode) body.get("fields")).set(entry.getKey(), entry.getValue()));
                } catch (JsonProcessingException e) {
                    throw new JiraException("Invalid transitionFields JSON: " + e.getMessage());
                }
            }

            String url = config.jiraBaseUrl() + "/issue/" + config.getIssueKey() + "/transitions";
            HttpResponse<String> response = sendRequest("POST", url, body.toString());
            if (response.statusCode() == 204 || response.statusCode() == 200) {
                // Fetch new status
                String newStatus = "";
                try {
                    GetIssueResult issue = getIssue(JiraConfiguration.builder()
                            .atlassianDomain(config.getAtlassianDomain())
                            .authMode(config.getAuthMode())
                            .userEmail(config.getUserEmail())
                            .apiToken(config.getApiToken())
                            .oauthClientId(config.getOauthClientId())
                            .oauthClientSecret(config.getOauthClientSecret())
                            .oauthRefreshToken(config.getOauthRefreshToken())
                            .connectTimeout(config.getConnectTimeout())
                            .readTimeout(config.getReadTimeout())
                            .issueKey(config.getIssueKey())
                            .fields("status")
                            .build());
                    newStatus = issue.statusName();
                } catch (JiraException e) {
                    log.warn("Could not fetch new status after transition: {}", e.getMessage());
                }
                return new TransitionIssueResult(config.getIssueKey(), newStatus, actualTransitionId);
            }
            parseResponse(response, 204);
            return new TransitionIssueResult(config.getIssueKey(), "", actualTransitionId);
        });
    }

    public AddCommentResult addComment(JiraConfiguration config) throws JiraException {
        return retryPolicy.execute(() -> {
            ObjectNode body = objectMapper.createObjectNode();
            body.set("body", markdownToAdf(config.getComment()));

            // Handle visibility
            if (config.getVisibility() != null && !config.getVisibility().isBlank()) {
                ObjectNode visNode = body.putObject("visibility");
                String vis = config.getVisibility();
                if (vis.startsWith("role:")) {
                    visNode.put("type", "role");
                    visNode.put("value", vis.substring(5));
                } else if (vis.startsWith("group:")) {
                    visNode.put("type", "group");
                    visNode.put("value", vis.substring(6));
                }
            }

            String url = config.jiraBaseUrl() + "/issue/" + config.getIssueKey() + "/comment";
            HttpResponse<String> response = sendRequest("POST", url, body.toString());
            JsonNode json = parseResponse(response, 201);

            return new AddCommentResult(
                    json.path("id").asText(),
                    config.getIssueKey(),
                    json.path("created").asText(""));
        });
    }

    public SearchIssuesResult searchIssues(JiraConfiguration config) throws JiraException {
        return retryPolicy.execute(() -> {
            List<JsonNode> allIssues = new ArrayList<>();
            int currentStartAt = config.getStartAt();
            int total = 0;
            boolean hasMore;

            do {
                ObjectNode body = objectMapper.createObjectNode();
                body.put("jql", config.getJqlQuery());
                body.put("startAt", currentStartAt);
                body.put("maxResults", config.getMaxResults());
                if (config.getFields() != null && !config.getFields().isBlank() && !"*all".equals(config.getFields())) {
                    ArrayNode fieldsArr = body.putArray("fields");
                    Arrays.stream(config.getFields().split(","))
                            .map(String::trim).forEach(fieldsArr::add);
                }

                String url = config.jiraBaseUrl() + "/search";
                HttpResponse<String> response = sendRequest("POST", url, body.toString());
                JsonNode json = parseResponse(response, 200);

                total = json.path("total").asInt(0);
                JsonNode issues = json.path("issues");
                if (issues.isArray()) {
                    issues.forEach(allIssues::add);
                }

                currentStartAt += config.getMaxResults();
                hasMore = currentStartAt < total && currentStartAt < config.getMaxTotalResults();
            } while (config.isFetchAllPages() && hasMore);

            // Build summary array
            ArrayNode summaryArray = objectMapper.createArrayNode();
            List<String> keys = new ArrayList<>();
            for (JsonNode issue : allIssues) {
                ObjectNode summary = objectMapper.createObjectNode();
                summary.put("key", issue.path("key").asText());
                JsonNode f = issue.path("fields");
                summary.put("summary", f.path("summary").asText(""));
                summary.put("statusName", f.path("status").path("name").asText(""));
                summary.put("statusCategory", f.path("status").path("statusCategory").path("key").asText(""));
                summary.put("priorityName", f.path("priority").path("name").asText(""));
                summary.put("assigneeDisplayName", f.path("assignee").path("displayName").asText(""));
                summary.put("assigneeAccountId", f.path("assignee").path("accountId").asText(""));
                summary.put("issueTypeName", f.path("issuetype").path("name").asText(""));
                summary.put("created", f.path("created").asText(""));
                summary.put("updated", f.path("updated").asText(""));
                summaryArray.add(summary);
                keys.add(issue.path("key").asText());
            }

            boolean morePages = currentStartAt < total;
            return new SearchIssuesResult(
                    summaryArray.toString(),
                    total,
                    allIssues.size(),
                    String.join(",", keys),
                    morePages);
        });
    }

    public CreateConfluencePageResult createConfluencePage(JiraConfiguration config) throws JiraException {
        return retryPolicy.execute(() -> {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("spaceId", config.getSpaceId());
            body.put("status", config.getStatus() != null ? config.getStatus() : "current");
            body.put("title", config.getPageTitle());

            ObjectNode bodyContent = body.putObject("body");
            ObjectNode repr = bodyContent.putObject(
                    config.getContentFormat() != null ? config.getContentFormat() : "storage");
            repr.put("representation",
                    config.getContentFormat() != null ? config.getContentFormat() : "storage");
            repr.put("value", config.getPageContent());

            if (config.getParentPageId() != null && !config.getParentPageId().isBlank()) {
                body.put("parentId", config.getParentPageId());
            }

            String url = config.confluenceBaseUrl() + "/pages";
            HttpResponse<String> response = sendRequest("POST", url, body.toString());
            JsonNode json = parseResponse(response, 200);

            String pageId = json.path("id").asText();
            String pageTitle = json.path("title").asText();
            int pageVersion = json.path("version").path("number").asInt(1);
            String spaceKey = json.path("spaceId").asText("");
            String pageUrl = config.wikiBaseUrl() + json.path("_links").path("webui").asText("");

            // Apply labels if provided
            if (config.getLabels() != null && !config.getLabels().isBlank()) {
                try {
                    ArrayNode labelsBody = objectMapper.createArrayNode();
                    Arrays.stream(config.getLabels().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty())
                            .forEach(label -> {
                                ObjectNode labelNode = objectMapper.createObjectNode();
                                labelNode.put("prefix", "global");
                                labelNode.put("name", label);
                                labelsBody.add(labelNode);
                            });
                    String labelUrl = config.confluenceBaseUrl() + "/pages/" + pageId + "/labels";
                    sendRequest("POST", labelUrl, labelsBody.toString());
                } catch (Exception e) {
                    log.warn("Failed to apply labels to Confluence page: {}", e.getMessage());
                }
            }

            return new CreateConfluencePageResult(pageId, pageTitle, pageUrl, pageVersion);
        });
    }

    public GetIssueTransitionsResult getIssueTransitions(JiraConfiguration config) throws JiraException {
        return retryPolicy.execute(() -> {
            String url = config.jiraBaseUrl() + "/issue/" + config.getIssueKey() + "/transitions";
            HttpResponse<String> response = sendRequest("GET", url, null);
            JsonNode json = parseResponse(response, 200);

            ArrayNode transitionsArr = (ArrayNode) json.path("transitions");
            List<String> names = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            ArrayNode outputArr = objectMapper.createArrayNode();

            if (transitionsArr != null && transitionsArr.isArray()) {
                for (JsonNode t : transitionsArr) {
                    ObjectNode entry = objectMapper.createObjectNode();
                    String id = t.path("id").asText();
                    String name = t.path("name").asText();
                    entry.put("id", id);
                    entry.put("name", name);
                    ObjectNode to = entry.putObject("to");
                    to.put("name", t.path("to").path("name").asText(""));
                    to.put("statusCategory", t.path("to").path("statusCategory").path("key").asText(""));
                    entry.put("hasScreen", t.path("hasScreen").asBoolean(false));
                    entry.set("fields", t.path("fields"));
                    outputArr.add(entry);
                    names.add(name);
                    ids.add(id);
                }
            }

            return new GetIssueTransitionsResult(
                    outputArr.toString(),
                    names.size(),
                    String.join(",", names),
                    String.join(",", ids));
        });
    }

    // ========== HTTP Infrastructure ==========

    private HttpResponse<String> sendRequest(String method, String url, String body) throws JiraException {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Authorization", getAuthorizationHeader())
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()));

            if (body != null) {
                reqBuilder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            } else {
                reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            log.debug("{} {}", method, url);
            return httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            throw new JiraException("Request timed out: " + url, e);
        } catch (java.io.IOException e) {
            throw new JiraException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraException("Request interrupted", e);
        }
    }

    private JsonNode parseResponse(HttpResponse<String> response, int expectedStatus) throws JiraException {
        int status = response.statusCode();
        if (status == expectedStatus) {
            try {
                return objectMapper.readTree(response.body());
            } catch (JsonProcessingException e) {
                throw new JiraException("Failed to parse response JSON: " + e.getMessage());
            }
        }

        // Handle error responses
        String errorMsg = extractErrorMessage(response);
        boolean retryable = RetryPolicy.isRetryableStatusCode(status);

        if (status == 401) {
            if ("OAUTH2".equalsIgnoreCase(configuration.getAuthMode())) {
                throw new JiraException("OAuth2 authentication failed -- verify client credentials and refresh token",
                        status, false);
            }
            throw new JiraException("Authentication failed -- verify API token is valid and not revoked",
                    status, false);
        }
        if (status == 403) {
            throw new JiraException("Forbidden -- user lacks required permission. " + errorMsg, status, false);
        }
        if (status == 404) {
            throw new JiraException("Not found: " + errorMsg, status, false);
        }
        if (status == 409) {
            throw new JiraException("Conflict -- concurrent edit detected. " + errorMsg, status, true);
        }

        throw new JiraException("HTTP " + status + ": " + errorMsg, status, retryable);
    }

    private String extractErrorMessage(HttpResponse<String> response) {
        try {
            JsonNode json = objectMapper.readTree(response.body());
            // JIRA error format
            if (json.has("errorMessages")) {
                ArrayNode msgs = (ArrayNode) json.get("errorMessages");
                List<String> errors = new ArrayList<>();
                msgs.forEach(n -> errors.add(n.asText()));
                if (json.has("errors") && json.get("errors").isObject()) {
                    json.get("errors").fields().forEachRemaining(entry ->
                            errors.add(entry.getKey() + ": " + entry.getValue().asText()));
                }
                return String.join("; ", errors);
            }
            if (json.has("message")) {
                return json.get("message").asText();
            }
        } catch (Exception ignored) {
            // Fall through to raw body
        }
        String body = response.body();
        return body != null && body.length() > 500 ? body.substring(0, 500) : (body != null ? body : "");
    }

    private String getAuthorizationHeader() throws JiraException {
        if ("OAUTH2".equalsIgnoreCase(configuration.getAuthMode())) {
            if (Instant.now().isAfter(tokenExpiry.minusSeconds(300))) {
                refreshOAuth2Token();
            }
            return "Bearer " + accessToken;
        }
        // Basic auth
        String email = configuration.getUserEmail();
        String token = configuration.getApiToken();
        // Fallback to system/env
        if (email == null || email.isBlank()) {
            email = System.getProperty("atlassian.userEmail");
            if (email == null) email = System.getenv("ATLASSIAN_USER_EMAIL");
        }
        if (token == null || token.isBlank()) {
            token = System.getProperty("atlassian.apiToken");
            if (token == null) token = System.getenv("ATLASSIAN_API_TOKEN");
        }
        String credentials = email + ":" + token;
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }

    private void refreshOAuth2Token() throws JiraException {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("grant_type", "refresh_token");
            body.put("client_id", configuration.getOauthClientId());
            body.put("client_secret", configuration.getOauthClientSecret());
            body.put("refresh_token", configuration.getOauthRefreshToken());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://auth.atlassian.com/oauth/token"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new JiraException("OAuth2 token refresh failed: HTTP " + response.statusCode(),
                        response.statusCode(), false);
            }

            JsonNode json = objectMapper.readTree(response.body());
            this.accessToken = json.path("access_token").asText();
            int expiresIn = json.path("expires_in").asInt(3600);
            this.tokenExpiry = Instant.now().plusSeconds(expiresIn);
            log.debug("OAuth2 token refreshed, expires in {}s", expiresIn);
        } catch (JiraException e) {
            throw e;
        } catch (Exception e) {
            throw new JiraException("Failed to refresh OAuth2 token: " + e.getMessage(), e);
        }
    }

    // ========== ADF Conversion ==========

    /**
     * Converts Markdown to a simple ADF document node.
     * Supports plain text wrapping in paragraph nodes.
     */
    JsonNode markdownToAdf(String markdown) {
        ObjectNode doc = objectMapper.createObjectNode();
        doc.put("version", 1);
        doc.put("type", "doc");
        ArrayNode content = doc.putArray("content");

        if (markdown == null || markdown.isBlank()) {
            ObjectNode para = content.addObject();
            para.put("type", "paragraph");
            para.putArray("content");
            return doc;
        }

        // Simple line-by-line conversion
        String[] lines = markdown.split("\n");
        for (String line : lines) {
            if (line.startsWith("# ")) {
                addHeading(content, line.substring(2).trim(), 1);
            } else if (line.startsWith("## ")) {
                addHeading(content, line.substring(3).trim(), 2);
            } else if (line.startsWith("### ")) {
                addHeading(content, line.substring(4).trim(), 3);
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                ObjectNode listItem = content.addObject();
                listItem.put("type", "bulletList");
                ArrayNode listContent = listItem.putArray("content");
                ObjectNode item = listContent.addObject();
                item.put("type", "listItem");
                ArrayNode itemContent = item.putArray("content");
                addParagraph(itemContent, line.substring(2).trim());
            } else if (!line.isBlank()) {
                addParagraph(content, line);
            }
        }

        if (!content.iterator().hasNext()) {
            ObjectNode para = content.addObject();
            para.put("type", "paragraph");
            para.putArray("content");
        }

        return doc;
    }

    private void addHeading(ArrayNode content, String text, int level) {
        ObjectNode heading = content.addObject();
        heading.put("type", "heading");
        ObjectNode attrs = heading.putObject("attrs");
        attrs.put("level", level);
        ArrayNode hContent = heading.putArray("content");
        ObjectNode textNode = hContent.addObject();
        textNode.put("type", "text");
        textNode.put("text", text);
    }

    private void addParagraph(ArrayNode content, String text) {
        ObjectNode para = content.addObject();
        para.put("type", "paragraph");
        ArrayNode pContent = para.putArray("content");
        ObjectNode textNode = pContent.addObject();
        textNode.put("type", "text");
        textNode.put("text", text);
    }

    /**
     * Converts ADF JSON to plain text by extracting all text nodes.
     */
    String adfToPlainText(JsonNode adf) {
        if (adf == null || adf.isMissingNode() || adf.isNull()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        extractText(adf, sb);
        return sb.toString().trim();
    }

    private void extractText(JsonNode node, StringBuilder sb) {
        if (node.has("text")) {
            sb.append(node.get("text").asText());
        }
        if (node.has("content") && node.get("content").isArray()) {
            for (JsonNode child : node.get("content")) {
                extractText(child, sb);
            }
            // Add newline after block elements
            String type = node.path("type").asText("");
            if ("paragraph".equals(type) || type.startsWith("heading")) {
                sb.append("\n");
            }
        }
    }

    private String findTransitionIdByName(GetIssueTransitionsResult transitions, String name) throws JiraException {
        try {
            ArrayNode arr = (ArrayNode) objectMapper.readTree(transitions.transitions());
            for (JsonNode t : arr) {
                if (t.path("name").asText("").equalsIgnoreCase(name)) {
                    return t.path("id").asText();
                }
            }
        } catch (JsonProcessingException e) {
            throw new JiraException("Failed to parse transitions: " + e.getMessage());
        }
        throw new JiraException("Transition '" + name + "' is not available from current status. " +
                "Available transitions: " + transitions.transitionNames());
    }

    // ========== Result Records ==========

    public record CreateIssueResult(String issueId, String issueKey, String issueSelf, String issueUrl) {}
    public record UpdateIssueResult(String issueKey) {}
    public record GetIssueResult(
            String issueKey, String issueId, String summary, String description,
            String statusName, String statusCategory, String priorityName,
            String assigneeDisplayName, String assigneeAccountId, String reporterDisplayName,
            String issueTypeName, String projectKey, String labels,
            String created, String updated, String dueDate, String resolution,
            int commentCount, String issueJson, String issueUrl) {}
    public record TransitionIssueResult(String issueKey, String newStatus, String transitionId) {}
    public record AddCommentResult(String commentId, String issueKey, String created) {}
    public record SearchIssuesResult(
            String issues, int totalResults, int returnedResults,
            String issueKeys, boolean hasMorePages) {}
    public record CreateConfluencePageResult(String pageId, String pageTitle, String pageUrl, int pageVersion) {}
    public record GetIssueTransitionsResult(
            String transitions, int transitionCount,
            String transitionNames, String transitionIds) {}
}
