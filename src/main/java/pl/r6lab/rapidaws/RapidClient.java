package pl.r6lab.rapidaws;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final class RapidClient {

    private static final String AWS_ACCESS_KEY_ENV_VARIABLE = "AWS_ACCESS_KEY";
    private static final String AWS_SECRET_KEY_ENV_VARIABLE = "AWS_SECRET_KEY";
    private static final String AWS_SESSION_TOKEN_ENV_VARIABLE = "AWS_SESSION_TOKEN";
    private static final String AWS_REGION_ENV_VARIABLE = "AWS_REGION";

    private static final String SIGNATURE_KEY_DATE_PATTERN = "YYYYMMdd";
    private static final String AWS_DATE_PATTERN = "YYYYMMdd'T'HHmmss'Z'";
    private static final DateTimeFormatter SIGNATURE_KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern(SIGNATURE_KEY_DATE_PATTERN);
    private static final DateTimeFormatter AWS_DATE_FORMATTER = DateTimeFormatter.ofPattern(AWS_DATE_PATTERN);

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String SIGNED_HEADERS = "content-length;content-type;host;x-amz-date;x-amz-target";
    private static final String CANONICAL_URI = "/";
    private static final String NEW_LINE = "\n";
    private static final String DOT = ".";
    private static final String X_AMZ_SECURITY_TOKEN = "x-amz-security-token";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final String accessKey;
    private final String secretKey;
    private final String sessionToken;
    private final String region;

    private RapidClient(String accessKey, String secretKey, String sessionToken, String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.sessionToken = sessionToken;
        this.region = region;
    }

    public final Response execute(Request request) {
        LocalDateTime now = LocalDateTime.now();
        String signatureDate = now.toLocalDate().format(SIGNATURE_KEY_DATE_FORMATTER);
        String awsDate = now.format(AWS_DATE_FORMATTER);

        try {
            HttpURLConnection connection = initConnection(request.getServiceName(), request.getMethod());
            byte[] signingKey = SignatureVersion4.getSignatureKey(this.secretKey, now.toLocalDate(), this.region, request.getServiceName().getName());
            int contentLength = request.getPayload().getBytes().length;
            setBasicHeaders(connection, request, awsDate, contentLength);

            // Used for Temporary Security Credentials
            if (nonNull(this.sessionToken)) {
                connection.setRequestProperty(X_AMZ_SECURITY_TOKEN, sessionToken);
            }

            String stringToSign = stringToSign(contentLength, awsDate, signatureDate, request);
            String signature = signature(signingKey, stringToSign);
            String authorizationHeader = authorizationHeader(signature, signatureDate, request.getServiceName().getName());

            connection.setRequestProperty(AUTHORIZATION_HEADER, authorizationHeader);

            connection.getOutputStream().write(request.getPayload().getBytes());
            connection.getOutputStream().flush();
            return handleResponse(connection);
        } catch (Exception e) {
            throw new RapidClientException(e);
        }
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

    private HttpURLConnection initConnection(ServiceName serviceName, HttpMethod method) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://" + host(serviceName.getName())).openConnection();
            connection.setRequestMethod(method.name());
            connection.setDoOutput(true);
            return connection;
        } catch (IOException e) {
            throw new RapidClientException(e);
        }
    }

    private Response handleResponse(HttpURLConnection connection) throws IOException {
        if (connection.getResponseCode() == 200) {
            return Response.success(getResponse(connection.getInputStream()));
        } else {
            return Response.fail(getResponse(connection.getErrorStream()));
        }
    }

    private void setBasicHeaders(HttpURLConnection connection, Request request, String awsDate, int contentLength) {
        connection.setRequestProperty("Content-Length", Integer.toString(contentLength));
        connection.setRequestProperty("Content-Type", request.getServiceName().getContentType());
        connection.setRequestProperty("Host", host(request.getServiceName().getName()));
        connection.setRequestProperty("X-Amz-Date", awsDate);
        connection.setRequestProperty("X-Amz-Target", request.getServiceName().getVersion() + DOT + request.getAction());
    }

    private String getResponse(InputStream inputStream) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(inputStream));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        return content.toString();
    }

    private String stringToSign(int contentLength, String awsDate, String signatureDate, Request request) throws NoSuchAlgorithmException {
        String canonicalRequest = hexBinary(SignatureVersion4.sha256(canonicalRequest(contentLength, awsDate, request))).toLowerCase();
        return ALGORITHM + NEW_LINE + awsDate + NEW_LINE + credentialsScope(signatureDate, request.getServiceName().getName()) + NEW_LINE + canonicalRequest;
    }

    private String authorizationHeader(String signature, String signatureDate, String serviceName) {
        return ALGORITHM + " Credential=" + this.accessKey + '/' + credentialsScope(signatureDate, serviceName) + ", SignedHeaders=" + SIGNED_HEADERS + ", Signature=" + signature;
    }

    private String canonicalRequest(int contentLength, String awsDate, Request request) throws NoSuchAlgorithmException {
        String hashedPayload = hexBinary(SignatureVersion4.sha256(request.getPayload())).toLowerCase();
        return request.getMethod().name() + NEW_LINE
                + CANONICAL_URI + NEW_LINE + NEW_LINE
                + "content-length:" + contentLength + NEW_LINE
                + "content-type:" + request.getServiceName().getContentType() + NEW_LINE
                + canonicalHeaders(awsDate, request.getServiceName().getName()) + NEW_LINE
                + "x-amz-target:" + request.getServiceName().getVersion() + DOT + request.getAction() + NEW_LINE
                + NEW_LINE + SIGNED_HEADERS + NEW_LINE
                + hashedPayload;
    }

    private String signature(byte[] signingKey, String stringToSign) throws Exception {
        return hexBinary(SignatureVersion4.hmacSHA256(stringToSign, signingKey)).toLowerCase();
    }

    private String hexBinary(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }

    private String canonicalHeaders(String awsDate, String serviceName) {
        return "host:" + host(serviceName) + NEW_LINE + "x-amz-date:" + awsDate;
    }

    private String credentialsScope(String signatureDate, String serviceName) {
        return signatureDate + "/" + region + "/" + serviceName + "/" + "aws4_request";
    }

    private String host(String serviceName) {
        return serviceName + DOT + this.region + DOT + "amazonaws.com";
    }


}
