package pl.r6lab.rapidaws.httpclient;

import pl.r6lab.rapidaws.AbstractRapidClient;
import pl.r6lab.rapidaws.RapidClientConfiguration;
import pl.r6lab.rapidaws.RapidClientException;
import pl.r6lab.rapidaws.Request;
import pl.r6lab.rapidaws.Response;
import pl.r6lab.rapidaws.SignatureVersion4;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public final class HttpClientRapidClient extends AbstractRapidClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final RapidClientConfiguration configuration;

    public HttpClientRapidClient(RapidClientConfiguration configuration) {
        super(configuration.getRegion());
        this.configuration = configuration;
    }

    public final Response execute(Request request) {
        LocalDateTime now = LocalDateTime.now();
        String signatureDate = now.toLocalDate().format(SIGNATURE_KEY_DATE_FORMATTER);
        String awsDate = now.format(AWS_DATE_FORMATTER);
        List<String> headers = new ArrayList<>();

        try {

            byte[] signingKey = SignatureVersion4.getSignatureKey(configuration.getSecretKey(), now.toLocalDate(), getRegion(), request.getServiceName().getName());
            int contentLength = configuration.payload(request).getBytes().length;
            configuration.buildBasicHeaders(request, awsDate, contentLength).entrySet().forEach(e -> {
                headers.add(e.getKey());
                headers.add(e.getValue());
            });

            // Used for Temporary Security Credentials
            if (nonNull(configuration.getSessionToken())) {
                headers.add(X_AMZ_SECURITY_TOKEN);
                headers.add(configuration.getSessionToken());
            }

            String stringToSign = configuration.stringToSign(contentLength, awsDate, signatureDate, request);
            if (isPrintHeaders()) {
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

            headers.add(AUTHORIZATION_HEADER);
            headers.add(authorizationHeader);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(configuration.endpointUrl(request)))
                    .headers(headers.toArray(headers.stream().toArray(String[]::new)))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(configuration.payload(request).getBytes()))
                    .build();
            final HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (Exception e) {
            throw new RapidClientException(e);
        }
    }

    private Response handleResponse(HttpResponse<String> response) throws IOException {
        return response.statusCode() == 200 ? Response.success(response.body()) : Response.fail(response.body());
    }

}
