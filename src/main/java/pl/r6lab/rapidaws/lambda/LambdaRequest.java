package pl.r6lab.rapidaws.lambda;

import pl.r6lab.rapidaws.HttpMethod;

public final class LambdaRequest {

    private final String functionName;
    private final String payload;
    private final HttpMethod method;

    private LambdaRequest(String functionName, String payload, HttpMethod method) {
        this.functionName = functionName;
        this.payload = payload;
        this.method = method;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getPayload() {
        return payload;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public static final LambdaRequest of(String functionName, String payload, HttpMethod method) {
        return new LambdaRequest(functionName, payload, method);
    }
}
