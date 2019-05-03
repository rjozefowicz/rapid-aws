package pl.r6lab.rapidaws;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.nonNull;

public abstract class AbstractRapidClient {

    private static final int DEFAULT_SOCKETS_NUMBER = 5;
    private static final Logger log = Logger.getGlobal();

    static {
        try {
            String initialSockets = System.getenv("INITIAL_SOCKETS");
            HttpsURLConnection.setDefaultSSLSocketFactory(new BufferedSSLSocketFactory(nonNull(initialSockets) ? Integer.valueOf(initialSockets) : DEFAULT_SOCKETS_NUMBER));
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to set default SSL Socket Factory");
            e.printStackTrace();
        }
    }

    protected static final String AWS_ACCESS_KEY_ENV_VARIABLE = "AWS_ACCESS_KEY";
    protected static final String AWS_SECRET_KEY_ENV_VARIABLE = "AWS_SECRET_KEY";
    protected static final String AWS_SESSION_TOKEN_ENV_VARIABLE = "AWS_SESSION_TOKEN";
    protected static final String AWS_REGION_ENV_VARIABLE = "AWS_REGION";

    private static final String SIGNATURE_KEY_DATE_PATTERN = "YYYYMMdd";
    private static final String AWS_DATE_PATTERN = "YYYYMMdd'T'HHmmss'Z'";
    private static final DateTimeFormatter SIGNATURE_KEY_DATE_FORMATTER = DateTimeFormatter.ofPattern(SIGNATURE_KEY_DATE_PATTERN);
    private static final DateTimeFormatter AWS_DATE_FORMATTER = DateTimeFormatter.ofPattern(AWS_DATE_PATTERN);

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String NEW_LINE = "\n";
    private static final String X_AMZ_SECURITY_TOKEN = "x-amz-security-token";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final String accessKey;
    private final String secretKey;
    private final String sessionToken;
    private final String region;

    private boolean printHeaders;

    protected AbstractRapidClient(String accessKey, String secretKey, String sessionToken, String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.sessionToken = sessionToken;
        this.region = region;
        this.printHeaders = false;
    }

    public final Response execute(Request request) {
        LocalDateTime now = LocalDateTime.now();
        String signatureDate = now.toLocalDate().format(SIGNATURE_KEY_DATE_FORMATTER);
        String awsDate = now.format(AWS_DATE_FORMATTER);

        try {
            PooledConnection connection = PooledConnection.newConnection(endpointUrl(request));
            byte[] signingKey = SignatureVersion4.getSignatureKey(this.secretKey, now.toLocalDate(), this.region, request.getServiceName().getName());
            int contentLength = payload(request).getBytes().length;
            setBasicHeaders(connection, request, awsDate, contentLength);

            // Used for Temporary Security Credentials
            if (nonNull(this.sessionToken)) {
                connection.addHeader(X_AMZ_SECURITY_TOKEN, sessionToken);
            }

            String stringToSign = stringToSign(contentLength, awsDate, signatureDate, request);
            if (this.printHeaders) {
                printHeader("String to sign", stringToSign);
            }

            String signature = signature(signingKey, stringToSign);
            if (this.printHeaders) {
                printHeader("Signature", signature);
            }

            String authorizationHeader = authorizationHeader(signature, signatureDate, request.getServiceName().getName());
            if (this.printHeaders) {
                printHeader("Authorization", authorizationHeader);
            }
            connection.addHeader(AUTHORIZATION_HEADER, authorizationHeader);

            String rawResponse = connection.execute(request.getMethod(), payload(request));
//            connection.getOutputStream().flush();
            return handleResponse(rawResponse);
        } catch (Exception e) {
            throw new RapidClientException(e);
        }
    }

    public void setPrintHeaders(boolean printHeaders) {
        this.printHeaders = printHeaders;
    }

    protected String getRegion() {
        return region;
    }

    protected abstract void setBasicHeaders(PooledConnection connection, Request request, String awsDate, int contentLength);

    protected abstract String canonicalRequest(int contentLength, String awsDate, Request request) throws NoSuchAlgorithmException;

    protected abstract String host(String serviceName);

    protected abstract String canonicalHeaders(String awsDate, String serviceName);

    protected abstract String endpointUrl(Request request);

    protected abstract String signedHeaders();

    protected abstract String payload(Request request);

    protected boolean isPrintHeaders() {
        return printHeaders;
    }

    protected void printHeader(String header, String value) {
        System.out.println(header);
        System.out.println(value);
        System.out.println("----------");
    }

//    private HttpsURLConnection initConnection(Request request) {
//        try {
//            PooledConnection pooledConnection = PooledConnection.newConnection(endpointUrl(request));
//            HttpsURLConnection connection = (HttpsURLConnection) new URL(endpointUrl(request)).openConnection();
//            connection.setRequestMethod(request.getMethod().name());
//            connection.setDoOutput(true);
//            return connection;
//        } catch (IOException e) {
//            throw new RapidClientException(e);
//        }
//    }

    private Response handleResponse(String rawResponse) throws IOException {
        System.out.println(rawResponse);
        return null;
//        if (connection.getResponseCode() == 200) {
//            return Response.success(getResponse(connection.getInputStream()));
//        } else {
//            return Response.fail(getResponse(connection.getErrorStream()));
//        }
    }

    private String stringToSign(int contentLength, String awsDate, String signatureDate, Request request) throws NoSuchAlgorithmException {
        String canonicalRequest = hexBinary(SignatureVersion4.sha256(canonicalRequest(contentLength, awsDate, request))).toLowerCase();
        return ALGORITHM + NEW_LINE + awsDate + NEW_LINE + credentialsScope(signatureDate, request.getServiceName().getName()) + NEW_LINE + canonicalRequest;
    }

    private String authorizationHeader(String signature, String signatureDate, String serviceName) {
        return ALGORITHM + " Credential=" + this.accessKey + '/' + credentialsScope(signatureDate, serviceName) + ", SignedHeaders=" + signedHeaders() + ", Signature=" + signature;
    }

    private String signature(byte[] signingKey, String stringToSign) throws Exception {
        return hexBinary(SignatureVersion4.hmacSHA256(stringToSign, signingKey)).toLowerCase();
    }

    protected String hexBinary(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }

    private String credentialsScope(String signatureDate, String serviceName) {
        return signatureDate + "/" + region + "/" + serviceName + "/" + "aws4_request";
    }

}
