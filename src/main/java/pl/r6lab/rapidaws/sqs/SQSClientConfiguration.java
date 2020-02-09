package pl.r6lab.rapidaws.sqs;

import pl.r6lab.rapidaws.RapidClientConfiguration;
import pl.r6lab.rapidaws.Request;
import pl.r6lab.rapidaws.ServiceName;
import pl.r6lab.rapidaws.SignatureVersion4;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static java.util.Objects.isNull;

public final class SQSClientConfiguration extends RapidClientConfiguration {

    private static final String SIGNED_HEADERS = "content-type;host;x-amz-date";
    private static final String NEW_LINE = "\n";
    private static final String DOT = ".";

    private SQSClientConfiguration(String accessKey, String secretKey, String sessionToken, String region) {
        super(accessKey, secretKey, sessionToken, region);
    }

    public final static SQSClientConfiguration envAware() {
        return new SQSClientConfiguration(System.getenv(AWS_ACCESS_KEY_ENV_VARIABLE), System.getenv(AWS_SECRET_KEY_ENV_VARIABLE), System.getenv(AWS_SESSION_TOKEN_ENV_VARIABLE), System.getenv(AWS_REGION_ENV_VARIABLE));
    }

    public final static SQSClientConfiguration of(String accessKey, String secretKey, String sessionToken, String region) {
        if (isNull(accessKey) || isNull(secretKey) || isNull(region)) {
            throw new IllegalArgumentException("Missing mandatory AWS parameters");
        }
        return new SQSClientConfiguration(accessKey, secretKey, sessionToken, region);
    }

    @Override
    public Map<String, String> buildBasicHeaders(Request request, String awsDate, int contentLength) {
        return Map.of(
                "Content-Type", ServiceName.SQS.getContentType(),
                "X-Amz-Host", host(ServiceName.SQS.getName()),
                "X-Amz-Date", awsDate
        );
    }

    @Override
    public String canonicalRequest(int contentLength, String awsDate, Request request) throws NoSuchAlgorithmException {
        SQSRequest sqsRequest = (SQSRequest) request;
        String hashedPayload = hexBinary(SignatureVersion4.sha256(payload(sqsRequest))).toLowerCase();
        String canonicalRequest = new StringBuilder()
                .append(request.getMethod().name())
                .append(NEW_LINE)
                .append(resourcePath(sqsRequest.getQueueUrl()))
                .append(NEW_LINE)
                .append(NEW_LINE)
                .append("content-type:")
                .append(ServiceName.SQS.getContentType())
                .append(NEW_LINE)
                .append(canonicalHeaders(awsDate, ServiceName.SQS.getName()))
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
        return ((SQSRequest) request).getQueueUrl();
    }

    @Override
    public String signedHeaders() {
        return SIGNED_HEADERS;
    }

    @Override
    public String payload(Request request) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("Action=")
                .append(request.getAction())
                .append("&Version=2012-11-05")
                .append("&MessageBody=")
                .append(request.getPayload().replace(" ", "+"));
        return stringBuilder.toString();
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

    private String resourcePath(String queueUrl) {
        String sqsEndpoint = "https://" + host(ServiceName.SQS.getName());
        return queueUrl.substring(queueUrl.indexOf(sqsEndpoint) + sqsEndpoint.length());
    }

}
