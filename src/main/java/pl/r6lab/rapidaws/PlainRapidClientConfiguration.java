package pl.r6lab.rapidaws;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static java.util.Objects.isNull;

public final class PlainRapidClientConfiguration extends RapidClientConfiguration {

    private static final String SIGNED_HEADERS = "content-length;content-type;host;x-amz-date;x-amz-target";
    private static final String CANONICAL_URI = "/";
    private static final String NEW_LINE = "\n";
    private static final String DOT = ".";

    private PlainRapidClientConfiguration(String accessKey, String secretKey, String sessionToken, String region) {
        super(accessKey, secretKey, sessionToken, region);
    }

    public final static PlainRapidClientConfiguration envAware() {
        return new PlainRapidClientConfiguration(System.getenv(AWS_ACCESS_KEY_ENV_VARIABLE), System.getenv(AWS_SECRET_KEY_ENV_VARIABLE), System.getenv(AWS_SESSION_TOKEN_ENV_VARIABLE), System.getenv(AWS_REGION_ENV_VARIABLE));
    }

    public final static PlainRapidClientConfiguration of(String accessKey, String secretKey, String sessionToken, String region) {
        if (isNull(accessKey) || isNull(secretKey) || isNull(region)) {
            throw new IllegalArgumentException("Missing mandatory AWS parameters");
        }
        return new PlainRapidClientConfiguration(accessKey, secretKey, sessionToken, region);
    }

    @Override
    public Map<String, String> buildBasicHeaders(Request request, String awsDate, int contentLength) {
        return Map.of(
                "X-Amz-Content-Length", Integer.toString(contentLength),
                "Content-Type", request.getServiceName().getContentType(),
                "X-Amz-Host", host(request.getServiceName().getName()),
                "X-Amz-Date", awsDate,
                "X-Amz-Target", request.getServiceName().getVersion() + DOT + request.getAction()
        );
    }

    @Override
    public String canonicalRequest(int contentLength, String awsDate, Request request) throws NoSuchAlgorithmException {
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
        return canonicalRequest;
    }

    @Override
    public String canonicalHeaders(String awsDate, String serviceName) {
        return new StringBuilder()
                .append("host:")
                .append(host(serviceName))
                .append(NEW_LINE)
                .append("x-amz-date:")
                .append(awsDate)
                .toString();
    }

    @Override
    public String endpointUrl(Request request) {
        return new StringBuilder()
                .append(getProtocol(request))
                .append(host(request.getServiceName().getName()))
                .toString();
    }

    @Override
    public String signedHeaders() {
        return SIGNED_HEADERS;
    }

    @Override
    public String payload(Request request) {
        return request.getPayload();
    }

    @Override
    public String host(String serviceName) {
        return new StringBuilder().append(serviceName)
                .append(DOT)
                .append(this.getRegion())
                .append(DOT)
                .append("amazonaws.com")
                .toString();
    }
}
