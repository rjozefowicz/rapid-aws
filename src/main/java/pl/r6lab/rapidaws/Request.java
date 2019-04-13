package pl.r6lab.rapidaws;

public interface Request {
    String getAction();

    String getPayload();

    ServiceName getServiceName();

    HttpMethod getMethod();
}
