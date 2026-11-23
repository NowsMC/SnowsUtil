package space.nows.util.net;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TcpProxyOddTest {
    private static Thread echoServer(ServerSocket server) {
        Thread t = new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    Socket s = server.accept();
                    new Thread(() -> echo(s)).start();
                } catch (IOException closed) {
                    return;
                }
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static void echo(Socket s) {
        try (s; InputStream in = s.getInputStream(); OutputStream out = s.getOutputStream()) {
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        } catch (IOException ignored) {
            // peer closed
        }
    }

    private static HostPort loopback(int port) {
        return HostPort.of(InetAddress.getLoopbackAddress().getHostAddress(), port);
    }

    @Test
    void forwardsBytesBothWays() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            echoServer(server);
            try (TcpProxy proxy = new TcpProxy(loopback(0), loopback(server.getLocalPort())).start()) {
                try (Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.port())) {
                    client.setSoTimeout(5000);
                    client.getOutputStream().write("hello proxy".getBytes());
                    byte[] reply = client.getInputStream().readNBytes("hello proxy".length());
                    assertEquals("hello proxy", new String(reply));
                }
                TcpProxy.Stats stats = proxy.stats();
                assertEquals(1, stats.accepted());
                assertTrue(stats.bytesToTarget() >= 11);
                assertTrue(stats.bytesFromTarget() >= 11);
            }
        }
    }

    @Test
    void rejectsConnectionsBeyondTheLimit() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            echoServer(server);
            TcpProxy.Options options = TcpProxy.Options.defaults().withMaxConnections(1);
            try (TcpProxy proxy = new TcpProxy(loopback(0), loopback(server.getLocalPort()), options).start()) {
                try (Socket first = new Socket(InetAddress.getLoopbackAddress(), proxy.port())) {
                    first.setSoTimeout(5000);
                    first.getOutputStream().write(1);
                    assertEquals(1, first.getInputStream().read());
                    try (Socket second = new Socket(InetAddress.getLoopbackAddress(), proxy.port())) {
                        second.setSoTimeout(5000);
                        assertEquals(-1, second.getInputStream().read(), "second connection must be closed");
                    }
                    assertTrue(proxy.stats().rejected() >= 1);
                }
            }
        }
    }

    @Test
    void closesIdleConnectionsAfterTheTimeout() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            echoServer(server);
            TcpProxy.Options options = TcpProxy.Options.defaults().withIdleTimeoutMillis(200);
            try (TcpProxy proxy = new TcpProxy(loopback(0), loopback(server.getLocalPort()), options).start();
                 Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.port())) {
                client.setSoTimeout(5000);
                assertEquals(-1, client.getInputStream().read(), "idle connection should be closed by the proxy");
            }
        }
    }

    @Test
    void unreachableTargetClosesTheClientInsteadOfHanging() throws Exception {
        int deadPort;
        try (ServerSocket probe = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            deadPort = probe.getLocalPort();
        }
        try (TcpProxy proxy = new TcpProxy(loopback(0), loopback(deadPort)).start();
             Socket client = new Socket(InetAddress.getLoopbackAddress(), proxy.port())) {
            client.setSoTimeout(5000);
            assertEquals(-1, client.getInputStream().read());
        }
    }

    @Test
    void closeStopsAcceptingAndStartIsNotRepeatable() throws Exception {
        TcpProxy proxy = new TcpProxy(loopback(0), loopback(9));
        proxy.start();
        int port = proxy.port();
        assertTrue(port > 0);
        assertThrows(IllegalStateException.class, proxy::start, "start twice must be refused");
        proxy.close();
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
            while (true) {
                try (Socket s = new Socket(InetAddress.getLoopbackAddress(), port)) {
                    s.setSoTimeout(200);
                    if (s.getInputStream().read() == -1) return;
                } catch (IOException refused) {
                    return;
                }
                TimeUnit.MILLISECONDS.sleep(20);
            }
        });
    }

    @Test
    void optionsRejectNonsense() {
        assertThrows(IllegalArgumentException.class, () -> TcpProxy.Options.defaults().withMaxConnections(0));
        assertThrows(IllegalArgumentException.class, () -> TcpProxy.Options.defaults().withBufferSize(0));
        assertThrows(IllegalArgumentException.class, () -> TcpProxy.Options.defaults().withConnectTimeoutMillis(-1));
        assertThrows(IllegalArgumentException.class, () -> TcpProxy.Options.defaults().withIdleTimeoutMillis(-1));
    }
}
