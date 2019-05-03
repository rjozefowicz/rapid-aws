package pl.r6lab.rapidaws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static java.net.InetAddress.getByName;

public class FunWithSocket {
    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {

        String endpoint = "localhost";
        String path = "/test";
        String payload = "{}";
//        InetAddress address = getByName(endpoint);
//        int port = 9000;
//        Socket socket = new Socket(address, port);
//
//        PrintWriter wr =
//                new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
//        postMethod(endpoint, path, payload, port, wr);
//        readOutput(socket);
//        Thread.sleep(2000);
//        payload = "{\"hello\":\"world\"}";
//        System.out.println("Send second message");
//        postMethod(endpoint, path, payload, port, wr);
//        readOutput(socket);


        PooledConnection pooledConnection = PooledConnection.newConnection(endpoint + path);
        pooledConnection.addHeader("Host", endpoint + ":" + 9000);
        pooledConnection.addHeader("Content-Length", Integer.toString(payload.length()));
        System.out.println(pooledConnection.execute(HttpMethod.POST, payload));
    }

    private static void readOutput(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String inputLine;
        boolean endOfChunks = false;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.equals("0")) {
                endOfChunks = true;
            } else if (inputLine.equals("") && endOfChunks) {
                break;
            }
            System.out.println(inputLine);
        }

    }

    private static void postMethod(String endpoint, String path, String payload, int port, PrintWriter wr) {
        wr.print("POST " + path + " HTTP/1.1\r\n");
        wr.print("Host: " + endpoint + ":" + port + "\r\n");
        wr.print("Content-Length: " + payload.length() + "\r\n");
//        wr.print("Connection: keep-alive\r\n");
        wr.print("\r\n");
        wr.print(payload);
        wr.print("\r\n");
        wr.flush();
    }
}
