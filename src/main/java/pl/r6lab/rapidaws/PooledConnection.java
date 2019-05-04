package pl.r6lab.rapidaws;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PooledConnection {

    private static final Map<String, PooledConnection> connections = new ConcurrentHashMap<>();

    private static final String NEW_LINE = "\r\n";
    private static final String HTTP_1_1 = "HTTP/1.1";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String KEEP_ALIVE = "keep-alive";

    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;

    private final String endpoint;
    private final BufferedReader reader;
    private final Socket socket;
    private Map<String, String> headers = new LinkedHashMap<>();
    private boolean keepAlive;

    private PooledConnection(String endpoint, Socket socket) throws IOException {
        socket.setKeepAlive(true);
        this.socket = socket;
        this.endpoint = endpoint;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public Response execute(HttpMethod method, String payload) throws IOException {
        writeRequest(method, payload);
        return handleResponse();
    }

    private boolean canReuse() {
        return this.socket.isConnected() && this.keepAlive;
    }

    private void writeRequest(HttpMethod method, String payload) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        String path = new URL(this.endpoint).getPath();
        sb.append(method.name() + " " + (path.equals("") ? "/" : path) + " " + HTTP_1_1 + NEW_LINE);
        headers.forEach((name, value) -> sb.append(name + ": " + value + NEW_LINE));
        if (headers.keySet().stream().noneMatch(header -> header.equalsIgnoreCase(CONTENT_LENGTH))) {
            sb.append(CONTENT_LENGTH + ": " + payload.length() + NEW_LINE);
        }
        sb.append(NEW_LINE);
        sb.append(payload);
        sb.append(NEW_LINE);
        try {
            this.socket.getOutputStream().write(sb.toString().getBytes(StandardCharsets.UTF_8));
            this.socket.getOutputStream().flush();
        } catch (Exception e) {
            System.out.println("Could not send messages");
        }
    }

    private Response handleResponse() throws IOException {
        boolean headersFinished = false;
        int contentLength = -1;
        int responseCode = -1;

        while (!headersFinished) {
            String line = reader.readLine();
            headersFinished = line.isEmpty();

            if (line.startsWith(HTTP_1_1)) {
                responseCode = Integer.parseInt(line.substring(HTTP_1_1.length() + 1, HTTP_1_1.length() + 4));
            } else if (line.startsWith(CONTENT_LENGTH)) {
                String cl = line.substring(CONTENT_LENGTH.length() + 2).trim();
                contentLength = Integer.parseInt(cl);
            } else if (line.contains(KEEP_ALIVE)) {
                this.keepAlive = true;
            }
        }

        char[] responseBody = new char[contentLength];
        reader.read(responseBody);

        String response = String.valueOf(responseBody);
        return responseCode == 200 ? Response.success(response) : Response.fail(response);
    }

    public static final PooledConnection newConnection(String endpoint, boolean httpsRequired) throws Exception {
        if (connections.containsKey(endpoint)) {
            PooledConnection connection = connections.get(endpoint);
            if (connection.canReuse()) {
                System.out.println("Reusing connection for endpoint: " + endpoint);
                return connection;
            } else {
                connections.remove(endpoint);
                return httpsRequired ? createHttpsConnection(endpoint) : createHttpConnection(endpoint);
            }
        }
        PooledConnection connection = httpsRequired ? createHttpsConnection(endpoint) : createHttpConnection(endpoint);
        connections.put(endpoint, connection);
        return connection;
    }

    private static PooledConnection createHttpConnection(String endpoint) throws Exception {
        Socket socket = new Socket(InetAddress.getByName(new URI(endpoint).getHost()), HTTP_PORT);
        return new PooledConnection(endpoint, socket);
    }

    private static PooledConnection createHttpsConnection(String endpoint) throws Exception {
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(InetAddress.getByName(new URI(endpoint).getHost()), HTTPS_PORT);
        socket.setTcpNoDelay(true);
        return new PooledConnection(endpoint, socket);
    }


}
