package pl.r6lab.rapidaws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

public final class PooledConnection {

    private static final int PORT = 80;
    private static final String NEW_LINE = "\r\n";
    private static final String HTTP_1_1 = "HTTP/1.1";
    private static final String CONTENT_LENGTH = "Content-Length: ";

    private final String endpoint;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private Map<String, String> headers = new LinkedHashMap<>();

    private PooledConnection(String endpoint, Socket socket) throws IOException {
        this.endpoint = endpoint;
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String execute(HttpMethod method, String payload) throws IOException {
        String path = new URL(this.endpoint).getPath();
        writer.print(method.name() + " " + (path.equals("") ? "/" : path) + " " + HTTP_1_1 + NEW_LINE);
        headers.forEach((name, value) -> writer.print(name + ": " + value + NEW_LINE));
        writer.print(NEW_LINE);
        writer.print(payload);
        writer.print(NEW_LINE);
        writer.flush();
        return readResponse();
    }

    private String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        String inputLine;
        int contentLength = -1;
        boolean rawResponseStarted = false;
        while (nonNull((inputLine = this.reader.readLine()))) {
            if (inputLine.length() == 0 && rawResponseStarted) {
                contentLength -= 1;
            }
            if (inputLine.startsWith(CONTENT_LENGTH)) {
                contentLength = Integer.valueOf(inputLine.substring(inputLine.indexOf(CONTENT_LENGTH) + CONTENT_LENGTH.length()));
            } else if (inputLine.isEmpty()) {
                rawResponseStarted = true;
            } else if (rawResponseStarted) {
                response.append(inputLine);
                response.append(NEW_LINE);
                contentLength -= inputLine.length() + 1;
                if (inputLine.isEmpty()) {
                    contentLength = -1;
                }
                if (0 == contentLength) {
                    break;
                }
            }
        }
        return response.toString();
    }

    public synchronized static final PooledConnection newConnection(String endpoint) throws IOException, URISyntaxException {
        return new PooledConnection(endpoint, new Socket(InetAddress.getByName(new URI(endpoint).getHost()), PORT));
    }

}
