package pl.r6lab.rapidaws.sqs;

import pl.r6lab.rapidaws.HttpMethod;

import java.util.Map;

public final class SQSRequest {

    private final String action;
    private final String payload;
    private final Map<String, String> params;
    private final String queueUrl;
    private final HttpMethod method;

    private SQSRequest(String action, String payload, Map<String, String> params, String queueUrl, HttpMethod method) {
        this.action = action;
        this.payload = payload;
        this.params = params;
        this.queueUrl = queueUrl;
        this.method = method;
    }

    public String getAction() {
        return action;
    }

    public String getPayload() {
        return payload;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getQueueUrl() {
        return queueUrl;
    }

    public static final SQSRequest of(String action, String payload, Map<String, String> params, String queueUrl, HttpMethod method) {
        return new SQSRequest(action, payload, params, queueUrl, method);
    }
}
