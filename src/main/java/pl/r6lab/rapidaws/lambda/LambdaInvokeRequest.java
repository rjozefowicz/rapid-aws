package pl.r6lab.rapidaws.lambda;

import pl.r6lab.rapidaws.HttpMethod;
import pl.r6lab.rapidaws.Request;
import pl.r6lab.rapidaws.ServiceName;

public final class LambdaInvokeRequest implements Request {

    private static final String INVOKE = "Invoke";
    private final ServiceName serviceName = ServiceName.LAMBDA;
    private final String functionName;
    private final String payload;
    private final HttpMethod method;

    private LambdaInvokeRequest(String functionName, String payload, HttpMethod method) {
        this.functionName = functionName;
        this.payload = payload;
        this.method = method;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String getAction() {
        return INVOKE;
    }

    public String getPayload() {
        return payload;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public static final LambdaInvokeRequest of(String functionName, String payload, HttpMethod method) {
        return new LambdaInvokeRequest(functionName, payload, method);
    }
}
