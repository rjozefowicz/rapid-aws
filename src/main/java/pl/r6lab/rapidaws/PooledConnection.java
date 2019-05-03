package pl.r6lab.rapidaws;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PooledConnection {

    private final static Map<String, PooledConnection> connections = new ConcurrentHashMap<>();

    private static final int PORT = 443;
    private static final String NEW_LINE = "\r\n";
    private static final String HTTP_1_1 = "HTTP/1.1";
    private static final String CONTENT_LENGTH = "Content-Length";

    private final String endpoint;
    private final PrintWriter writer;
    private final BufferedReader reader;
    private Map<String, String> headers = new LinkedHashMap<>();
    private boolean keepAlive;

    private PooledConnection(String endpoint, Socket socket) throws IOException {
        socket.setKeepAlive(true);
        this.endpoint = endpoint;
        this.writer = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()));
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String execute(HttpMethod method, String payload) throws IOException {
        String path = new URL(this.endpoint).getPath();
        writer.print(method.name() + " " + (path.equals("") ? "/" : path) + " " + HTTP_1_1 + NEW_LINE);
        headers.forEach((name, value) -> writer.print(name + ": " + value + NEW_LINE));
        if (headers.keySet().stream().noneMatch(header -> header.equalsIgnoreCase("content-length"))) {
            writer.print(CONTENT_LENGTH + ": " + payload.length() + NEW_LINE);
        }
        writer.print(NEW_LINE);
        writer.flush();
        writer.print(payload);
        writer.print(NEW_LINE);
        writer.flush();

        boolean headersFinished = false;
        int contentLength = -1;

        while (!headersFinished) {
            String line = reader.readLine();
            System.out.println(line);
            headersFinished = line.isEmpty();

            if (line.startsWith(CONTENT_LENGTH)) {
                String cl = line.substring(CONTENT_LENGTH.length() + 2).trim();
                contentLength = Integer.parseInt(cl);
            } else if (line.contains("keep-alive")) {
                this.keepAlive = true;
            }
        }

        char[] responseBody = new char[contentLength];
        reader.read(responseBody);

        return String.valueOf(responseBody);
    }

    public synchronized static final PooledConnection newConnection(String endpoint) throws IOException, URISyntaxException {
        if (connections.containsKey(endpoint)) {
            PooledConnection connection = connections.get(endpoint);
            if (connection.keepAlive) {
                System.out.println("Reusing connection for endpoint: " + endpoint);
                return connection;
            } else {
                connections.remove(endpoint);
                return new PooledConnection(endpoint, SSLSocketFactory.getDefault().createSocket(InetAddress.getByName(new URI(endpoint).getHost()), PORT));
            }
        }
        PooledConnection connection = new PooledConnection(endpoint, SSLSocketFactory.getDefault().createSocket(InetAddress.getByName(new URI(endpoint).getHost()), PORT));
        connections.put(endpoint, connection);
        return connection;
    }


}
