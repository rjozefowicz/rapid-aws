package pl.r6lab.rapidaws;

public class Request {
    private final ServiceName serviceName;
    private final String action;
    private final HttpMethod method;
    private final String payload;

    private Request(ServiceName serviceName, String action, HttpMethod method, String payload) {
        this.serviceName = serviceName;
        this.action = action;
        this.method = method;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public String getPayload() {
        return payload;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public static final Request of(ServiceName serviceName, String action, HttpMethod method, String payload) {
        return new Request(serviceName, action, method, payload);
    }
}
