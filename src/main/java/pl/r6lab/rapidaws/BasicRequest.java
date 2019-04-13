package pl.r6lab.rapidaws;

public class BasicRequest implements Request {
    private final ServiceName serviceName;
    private final String action;
    private final HttpMethod method;
    private final String payload;

    private BasicRequest(ServiceName serviceName, String action, HttpMethod method, String payload) {
        this.serviceName = serviceName;
        this.action = action;
        this.method = method;
        this.payload = payload;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @Override
    public ServiceName getServiceName() {
        return serviceName;
    }

    @Override
    public HttpMethod getMethod() {
        return method;
    }

    public static final BasicRequest of(ServiceName serviceName, String action, HttpMethod method, String payload) {
        return new BasicRequest(serviceName, action, method, payload);
    }
}
