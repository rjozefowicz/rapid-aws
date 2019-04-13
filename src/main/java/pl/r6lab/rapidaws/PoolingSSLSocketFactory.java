package pl.r6lab.rapidaws;

import sun.security.ssl.SSLSocketFactoryImpl;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class PoolingSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactoryImpl sslSocketFactory;
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();

    public PoolingSSLSocketFactory(int initialPoolSize) throws Exception {
        this.sslSocketFactory = new SSLSocketFactoryImpl();
        synchronized (this) {
            for (int i = 0; i < initialPoolSize; ++i) {
                sockets.add(this.sslSocketFactory.createSocket());
            }
        }
    }

    public Socket createSocket() {
        synchronized (this) {
            Optional<Socket> availableSocket = sockets.stream().filter(socket -> !socket.isConnected()).findAny();
            if (availableSocket.isPresent()) {
                Socket socket = availableSocket.get();

                return socket;
            }
        }
        Socket socket = this.sslSocketFactory.createSocket();
        sockets.add(socket);
        return socket;
    }

    public Socket createSocket(String var1, int var2) throws IOException, UnknownHostException {
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
