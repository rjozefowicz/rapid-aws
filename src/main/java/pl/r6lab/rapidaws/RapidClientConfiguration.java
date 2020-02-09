package pl.r6lab.rapidaws;

import javax.xml.bind.DatatypeConverter;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public abstract class RapidClientConfiguration {

    private static final String FORCE_HTTPS = "FORCE_HTTPS";

    protected static final String AWS_ACCESS_KEY_ENV_VARIABLE = "AWS_ACCESS_KEY";
    protected static final String AWS_SECRET_KEY_ENV_VARIABLE = "AWS_SECRET_KEY";
    protected static final String AWS_SESSION_TOKEN_ENV_VARIABLE = "AWS_SESSION_TOKEN";
    protected static final String AWS_REGION_ENV_VARIABLE = "AWS_REGION";

    protected static final String ALGORITHM = "AWS4-HMAC-SHA256";
    protected static final String NEW_LINE = "\n";

    private final String accessKey;
    private final String secretKey;
    private final String sessionToken;
    private final String region;
    private final Boolean forceHttps;

    private boolean printHeaders;

    protected RapidClientConfiguration(String accessKey, String secretKey, String sessionToken, String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.sessionToken = sessionToken;
        this.region = region;
        this.printHeaders = false;
        this.forceHttps = Boolean.valueOf(System.getenv(FORCE_HTTPS));
        System.out.println("Force HTTPS: " + this.forceHttps);
    }

    public abstract Map<String, String> buildBasicHeaders(Request request, String awsDate, int contentLength);

    public abstract String canonicalRequest(int contentLength, String awsDate, Request request) throws NoSuchAlgorithmException;

    public abstract String host(String serviceName);

    public abstract String canonicalHeaders(String awsDate, String serviceName);

    public abstract String endpointUrl(Request request);

    public abstract String signedHeaders();

    public abstract String payload(Request request);

    public boolean isPrintHeaders() {
        return printHeaders;
    }

    public boolean isForceHttps() {
        return forceHttps;
    }

    public String stringToSign(int contentLength, String awsDate, String signatureDate, Request request) throws NoSuchAlgorithmException {
        String canonicalRequest = hexBinary(SignatureVersion4.sha256(canonicalRequest(contentLength, awsDate, request))).toLowerCase();
        return ALGORITHM + NEW_LINE + awsDate + NEW_LINE + credentialsScope(signatureDate, request.getServiceName().getName()) + NEW_LINE + canonicalRequest;
    }

    public String authorizationHeader(String signature, String signatureDate, String serviceName) {
        return ALGORITHM + " Credential=" + this.accessKey + '/' + credentialsScope(signatureDate, serviceName) + ", SignedHeaders=" + signedHeaders() + ", Signature=" + signature;
    }

    public String signature(byte[] signingKey, String stringToSign) throws Exception {
        return hexBinary(SignatureVersion4.hmacSHA256(stringToSign, signingKey)).toLowerCase();
    }

    public String hexBinary(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }

    private String credentialsScope(String signatureDate, String serviceName) {
        return signatureDate + "/" + region + "/" + serviceName + "/" + "aws4_request";
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public Boolean getForceHttps() {
        return forceHttps;
    }

    public String getProtocol(Request request) {
        if (request.getServiceName().isHttpsRequired() || this.isForceHttps()) {
            return "https://";
        } else {
            return "http://";
        }
    }

    public String getRegion() {
        return region;
    }
}
