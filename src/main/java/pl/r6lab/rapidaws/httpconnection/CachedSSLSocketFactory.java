package pl.r6lab.rapidaws.httpconnection;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class CachedSSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory sslSocketFactory;
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();

    public CachedSSLSocketFactory(int initialPoolSize, final SSLSocketFactory defaultImplementation) throws Exception {
        this.sslSocketFactory = defaultImplementation;
        System.out.println("Number of initially created sockets: " + initialPoolSize);
        for (int i = 0; i < initialPoolSize; ++i) {
            sockets.add(this.sslSocketFactory.createSocket());
        }

    }

    public Socket createSocket() throws IOException {
        Optional<Socket> availableSocket = sockets.stream().filter(socket -> !socket.isConnected()).findAny();
        if (availableSocket.isPresent()) {
            return availableSocket.get();
        }

        Socket socket = this.sslSocketFactory.createSocket();
        sockets.add(socket);
        return socket;
    }

    public Socket createSocket(String var1, int var2) throws IOException {
        return this.sslSocketFactory.createSocket(var1, var2);
    }

    public Socket createSocket(Socket var1, String var2, int var3, boolean var4) throws IOException {
        return this.sslSocketFactory.createSocket(var1, var2, var3, var4);
    }

    public Socket createSocket(Socket var1, InputStream var2, boolean var3) throws IOException {
        return this.sslSocketFactory.createSocket(var1, var2, var3);
    }

    public Socket createSocket(InetAddress var1, int var2) throws IOException {
        return this.sslSocketFactory.createSocket(var1, var2);
    }

    public Socket createSocket(String var1, int var2, InetAddress var3, int var4) throws IOException {
        return this.sslSocketFactory.createSocket(var1, var2, var3, var4);
    }

    public Socket createSocket(InetAddress var1, int var2, InetAddress var3, int var4) throws IOException {
        return this.sslSocketFactory.createSocket(var1, var2, var3, var4);
    }

    public String[] getDefaultCipherSuites() {
        return this.sslSocketFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return this.sslSocketFactory.getSupportedCipherSuites();
    }
}
