package pl.r6lab.rapidaws.httpconnection;

import pl.r6lab.rapidaws.AbstractRapidClient;
import pl.r6lab.rapidaws.RapidClientConfiguration;
import pl.r6lab.rapidaws.RapidClientException;
import pl.r6lab.rapidaws.Request;
import pl.r6lab.rapidaws.Response;
import pl.r6lab.rapidaws.SignatureVersion4;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;

import static java.util.Objects.nonNull;

public final class HttpConnectionRapidClient extends AbstractRapidClient {

    private static final String INITIAL_SSL_SOCKETS = "INITIAL_SSL_SOCKETS";

    static {
        try {
            String initialSockets = System.getenv(INITIAL_SSL_SOCKETS);
            if (nonNull(initialSockets)) {
                final SSLSocketFactory defaultSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
                HttpsURLConnection.setDefaultSSLSocketFactory(new CachedSSLSocketFactory(Integer.valueOf(initialSockets), defaultSSLSocketFactory));
            }
        } catch (Exception e) {
            System.err.println("Unable to set default SSL Socket Factory");
            e.printStackTrace();
        }
    }

    private final RapidClientConfiguration configuration;

    public HttpConnectionRapidClient(RapidClientConfiguration configuration) {
        super(configuration.getRegion());
        this.configuration = configuration;
    }

    public final Response execute(Request request) {
        LocalDateTime now = LocalDateTime.now();
        String signatureDate = now.toLocalDate().format(SIGNATURE_KEY_DATE_FORMATTER);
        String awsDate = now.format(AWS_DATE_FORMATTER);

        HttpURLConnection connection = null;

        try {
            connection = initConnection(request);
            byte[] signingKey = SignatureVersion4.getSignatureKey(configuration.getSecretKey(), now.toLocalDate(), getRegion(), request.getServiceName().getName());
            int contentLength = configuration.payload(request).getBytes().length;
            for (Map.Entry<String, String> entry : configuration.buildBasicHeaders(request, awsDate, contentLength).entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            // Used for Temporary Security Credentials
            if (nonNull(configuration.getSessionToken())) {
                connection.setRequestProperty(X_AMZ_SECURITY_TOKEN, configuration.getSessionToken());
            }

            String stringToSign = configuration.stringToSign(contentLength, awsDate, signatureDate, request);
            if (configuration.isPrintHeaders()) {
                printHeader("String to sign", stringToSign);
            }

            String signature = configuration.signature(signingKey, stringToSign);
            if (isPrintHeaders()) {
                printHeader("Signature", signature);
            }

            String authorizationHeader = configuration.authorizationHeader(signature, signatureDate, request.getServiceName().getName());
            if (isPrintHeaders()) {
                printHeader("Authorization", authorizationHeader);
            }
            connection.setRequestProperty(AUTHORIZATION_HEADER, authorizationHeader);

            connection.getOutputStream().write(configuration.payload(request).getBytes());
            connection.getOutputStream().flush();
            return handleResponse(connection);
        } catch (Exception e) {
            throw new RapidClientException(e);
        } finally {
            if (nonNull(connection)) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection initConnection(Request request) {
        try {
            final String endpointUrl = configuration.endpointUrl(request);
            if (isPrintHeaders()) {
                printHeader("AWS Service endpoint", endpointUrl);
            }
            HttpURLConnection connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
            connection.setRequestMethod(request.getMethod().name());
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

}
