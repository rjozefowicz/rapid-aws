package pl.r6lab.rapidaws.lambda;

import pl.r6lab.rapidaws.AbstractRapidClient;
import pl.r6lab.rapidaws.PooledConnection;
import pl.r6lab.rapidaws.Request;
import pl.r6lab.rapidaws.ServiceName;
import pl.r6lab.rapidaws.SignatureVersion4;

import java.security.NoSuchAlgorithmException;

import static java.util.Objects.isNull;

public final class RapidLambdaInvocationClient extends AbstractRapidClient {

    private static final String SIGNED_HEADERS = "content-type;host;x-amz-date";
    private static final String NEW_LINE = "\n";
    private static final String DOT = ".";

    private RapidLambdaInvocationClient(String accessKey, String secretKey, String sessionToken, String region) {
        super(accessKey, secretKey, sessionToken, region);
    }

    public final static RapidLambdaInvocationClient envAware() {
        return new RapidLambdaInvocationClient(System.getenv(AWS_ACCESS_KEY_ENV_VARIABLE), System.getenv(AWS_SECRET_KEY_ENV_VARIABLE), System.getenv(AWS_SESSION_TOKEN_ENV_VARIABLE), System.getenv(AWS_REGION_ENV_VARIABLE));
    }

    public final static RapidLambdaInvocationClient of(String accessKey, String secretKey, String sessionToken, String region) {
        if (isNull(accessKey) || isNull(secretKey) || isNull(region)) {
            throw new IllegalArgumentException("Missing mandatory AWS parameters");
        }
        return new RapidLambdaInvocationClient(accessKey, secretKey, sessionToken, region);
    }

    @Override
    protected void setBasicHeaders(PooledConnection connection, Request request, String awsDate, int contentLength) {
        connection.addHeader("Content-Type", "");
        connection.addHeader("Host", host(ServiceName.LAMBDA.getName()));
        connection.addHeader("X-Amz-Date", awsDate);
    }

    @Override
    protected String canonicalRequest(int contentLength, String awsDate, Request request) throws NoSuchAlgorithmException {
        String hashedPayload = hexBinary(SignatureVersion4.sha256(request.getPayload())).toLowerCase();
        String canonicalRequest = new StringBuilder()
                .append(request.getMethod().name())
                .append(NEW_LINE)
                .append(resourcePath(((LambdaInvokeRequest) request).getFunctionName()))
                .append(NEW_LINE)
                .append(NEW_LINE)
                .append("content-type:")
                .append(NEW_LINE)
                .append(canonicalHeaders(awsDate, ServiceName.LAMBDA.getName()))
                .append(NEW_LINE)
                .append(NEW_LINE)
                .append(SIGNED_HEADERS)
                .append(NEW_LINE)
                .append(hashedPayload)
                .toString();
        if (this.isPrintHeaders()) {
            this.printHeader("Canonical request:", canonicalRequest);
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
                .append(host(ServiceName.LAMBDA.getName()))
                .append(resourcePath(((LambdaInvokeRequest) request).getFunctionName()))
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

    private String resourcePath(String functionName) {
        return new StringBuilder()
                .append("/2015-03-31/functions/")
                .append(functionName)
                .append("/invocations")
                .toString();
    }

}
