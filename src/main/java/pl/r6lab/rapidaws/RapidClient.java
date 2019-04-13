package pl.r6lab.rapidaws;

import java.net.HttpURLConnection;
import java.security.NoSuchAlgorithmException;

import static java.util.Objects.isNull;

public final class RapidClient extends AbstractRapidClient {

    private static final String SIGNED_HEADERS = "content-length;content-type;host;x-amz-date;x-amz-target";
    private static final String CANONICAL_URI = "/";
    private static final String NEW_LINE = "\n";
    private static final String DOT = ".";

    private RapidClient(String accessKey, String secretKey, String sessionToken, String region) {
        super(accessKey, secretKey, sessionToken, region);
    }

    public final static RapidClient envAware() {
        return new RapidClient(System.getenv(AWS_ACCESS_KEY_ENV_VARIABLE), System.getenv(AWS_SECRET_KEY_ENV_VARIABLE), System.getenv(AWS_SESSION_TOKEN_ENV_VARIABLE), System.getenv(AWS_REGION_ENV_VARIABLE));
    }

    public final static RapidClient of(String accessKey, String secretKey, String sessionToken, String region) {
        if (isNull(accessKey) || isNull(secretKey) || isNull(region)) {
            throw new IllegalArgumentException("Missing mandatory AWS parameters");
        }
        return new RapidClient(accessKey, secretKey, sessionToken, region);
    }

    @Override
    protected void setBasicHeaders(HttpURLConnection connection, Request request, String awsDate, int contentLength) {
        connection.setRequestProperty("Content-Length", Integer.toString(contentLength));
        connection.setRequestProperty("Content-Type", request.getServiceName().getContentType());
        connection.setRequestProperty("Host", host(request.getServiceName().getName()));
        connection.setRequestProperty("X-Amz-Date", awsDate);
        connection.setRequestProperty("X-Amz-Target", request.getServiceName().getVersion() + DOT + request.getAction());
    }

    @Override
    protected String canonicalRequest(int contentLength, String awsDate, Request request) throws NoSuchAlgorithmException {
        String hashedPayload = hexBinary(SignatureVersion4.sha256(request.getPayload())).toLowerCase();
        String canonicalRequest =
                new StringBuilder().append(request.getMethod().name())
                        .append(NEW_LINE)
                        .append(CANONICAL_URI)
                        .append(NEW_LINE)
                        .append(NEW_LINE)
                        .append("content-length:")
                        .append(contentLength)
                        .append(NEW_LINE)
                        .append("content-type:")
                        .append(request.getServiceName().getContentType())
                        .append(NEW_LINE)
                        .append(canonicalHeaders(awsDate, request.getServiceName().getName()))
                        .append(NEW_LINE)
                        .append("x-amz-target:")
                        .append(request.getServiceName().getVersion())
                        .append(DOT)
                        .append(request.getAction())
                        .append(NEW_LINE)
                        .append(NEW_LINE)
                        .append(SIGNED_HEADERS)
                        .append(NEW_LINE)
                        .append(hashedPayload)
                        .toString();
        if (this.isPrintHeaders()) {
            this.printHeader("Canonical request", canonicalRequest);
        }
        return canonicalRequest;
    }

    @Override
    protected String canonicalHeaders(String awsDate, String serviceName) {
        return new StringBuilder()
                .append("host:")
                .append(host(serviceName))
                .append(NEW_LINE)
                .append("x-amz-date:")
                .append(awsDate)
                .toString();
    }

    @Override
    protected String endpointUrl(Request request) {
        return new StringBuilder()
                .append("https://")
                .append(host(request.getServiceName().getName()))
                .toString();
    }

    @Override
    protected String signedHeaders() {
        return SIGNED_HEADERS;
    }

    @Override
    protected String payload(Request request) {
        return request.getPayload();
    }

    @Override
    protected String host(String serviceName) {
        return new StringBuilder().append(serviceName)
                .append(DOT)
                .append(this.getRegion())
                .append(DOT)
                .append("amazonaws.com")
                .toString();
    }


}
