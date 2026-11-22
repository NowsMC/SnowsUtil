package space.nows.util.net;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class TcpProxyTest {
    private static final HostPort ANY = HostPort.of("127.0.0.1", 0);

    interface Handler {
        void handle(Socket socket) throws Exception;
    }

    static final class TestServer implements AutoCloseable {
        final ServerSocket server;
        final List<Socket> sockets = new ArrayList<>();

        TestServer(Handler handler) throws IOException {
            server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread accept = new Thread(() -> {
                while (!server.isClosed()) {
                    try {
                        Socket s = server.accept();
                        synchronized (sockets) { sockets.add(s); }
                        Thread t = new Thread(() -> {
                            try {
                                handler.handle(s);
                            } catch (Exception ignored) {
                                closeQuietly(s);
                            }
                        });
                        t.setDaemon(true);
                        t.start();
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            accept.setDaemon(true);
            accept.start();
        }

        HostPort address() { return HostPort.of("127.0.0.1", server.getLocalPort()); }

        @Override public void close() {
            closeQuietly(server);
            synchronized (sockets) { sockets.forEach(TestServer::closeQuietly); }
        }

        static void closeQuietly(AutoCloseable c) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }

    static void echo(Socket s) throws IOException {
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
            out.flush();
        }
        s.shutdownOutput();
    }

    static Socket connect(TcpProxy proxy) throws IOException {
        Socket s = new Socket(InetAddress.getLoopbackAddress(), proxy.port());
        s.setSoTimeout(10_000);
        return s;
    }

    static boolean ended(Socket s) {
        try {
            return s.getInputStream().read() < 0;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    static void await(BooleanSupplier condition, long millis) throws InterruptedException {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) fail("condition not met within " + millis + " ms");
            Thread.sleep(10);
        }
    }

    @Test void relaysAnEchoRoundTrip() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            try (TestServer server = new TestServer(TcpProxyTest::echo);
                 TcpProxy proxy = new TcpProxy(ANY, server.address()).start();
                 Socket client = connect(proxy)) {
                client.getOutputStream().write("hello proxy".getBytes());
                byte[] reply = client.getInputStream().readNBytes(11);
                assertEquals("hello proxy", new String(reply));
                TcpProxy.Stats stats = proxy.stats();
                assertEquals(1, stats.accepted());
                assertEquals(0, stats.rejected());
            }
        });
    }

    @Test void relaysLargeTransfersIntactAndCountsBytes() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            byte[] data = new byte[4 * 1024 * 1024];
            new Random(1).nextBytes(data);
            try (TestServer server = new TestServer(TcpProxyTest::echo);
                 TcpProxy proxy = new TcpProxy(ANY, server.address()).start();
                 Socket client = connect(proxy)) {
                AtomicReference<Throwable> failure = new AtomicReference<>();
                Thread writer = new Thread(() -> {
                    try {
                        client.getOutputStream().write(data);
                        client.getOutputStream().flush();
                    } catch (Throwable t) {
                        failure.set(t);
                    }
                });
                writer.start();
                byte[] back = client.getInputStream().readNBytes(data.length);
                writer.join();
                assertNull(failure.get());
                assertArrayEquals(data, back);
                await(() -> proxy.stats().bytesFromTarget() >= data.length, 5_000);
                assertEquals(data.length, proxy.stats().bytesToTarget());
                assertEquals(data.length, proxy.stats().bytesFromTarget());
            }
        });
    }

    @Test void halfCloseDeliversTheRestOfTheReply() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            Handler countThenReply = s -> {
                long total = 0;
                InputStream in = s.getInputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) >= 0) total += n;
                s.getOutputStream().write(("received " + total).getBytes());
                s.close();
            };
            try (TestServer server = new TestServer(countThenReply);
                 TcpProxy proxy = new TcpProxy(ANY, server.address()).start();
                 Socket client = connect(proxy)) {
                client.getOutputStream().write(new byte[10_000]);
                client.shutdownOutput();
                ByteArrayOutputStream reply = new ByteArrayOutputStream();
                client.getInputStream().transferTo(reply);
                assertEquals("received 10000", reply.toString());
            }
        });
    }

    @Test void rejectsConnectionsOverTheLimitAndRecovers() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            TcpProxy.Options options = TcpProxy.Options.defaults().withMaxConnections(1);
            try (TestServer server = new TestServer(TcpProxyTest::echo);
                 TcpProxy proxy = new TcpProxy(ANY, server.address(), options).start()) {
                try (Socket first = connect(proxy)) {
                    first.getOutputStream().write(1);
                    assertEquals(1, first.getInputStream().read());
                    try (Socket second = connect(proxy)) {
                        assertTrue(ended(second));
                    }
                    assertEquals(1, proxy.stats().rejected());
                    assertEquals(1, proxy.stats().active());
                }
                await(() -> proxy.stats().active() == 0, 5_000);
                try (Socket third = connect(proxy)) {
                    third.getOutputStream().write(7);
                    assertEquals(7, third.getInputStream().read());
                }
            }
        });
    }

    @Test void closesQuietConnectionsAfterTheIdleTimeout() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            TcpProxy.Options options = TcpProxy.Options.defaults().withIdleTimeoutMillis(300);
            try (TestServer server = new TestServer(TcpProxyTest::echo);
                 TcpProxy proxy = new TcpProxy(ANY, server.address(), options).start();
                 Socket client = connect(proxy)) {
                client.getOutputStream().write(5);
                assertEquals(5, client.getInputStream().read());
                assertTrue(ended(client));
                await(() -> proxy.stats().active() == 0, 5_000);
            }
        });
    }

    @Test void trafficInOneDirectionKeepsTheConnectionAlive() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            Handler trickle = s -> {
                OutputStream out = s.getOutputStream();
                for (int i = 0; i < 12; i++) {
                    out.write(i);
                    out.flush();
                    Thread.sleep(100);
                }
                Thread.sleep(5_000);
            };
            TcpProxy.Options options = TcpProxy.Options.defaults().withIdleTimeoutMillis(400);
            try (TestServer server = new TestServer(trickle);
                 TcpProxy proxy = new TcpProxy(ANY, server.address(), options).start();
                 Socket client = connect(proxy)) {
                InputStream in = client.getInputStream();
                for (int i = 0; i < 12; i++) assertEquals(i, in.read());
            }
        });
    }

    @Test void unreachableTargetClosesTheClient() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            HostPort dead = HostPort.of("127.0.0.1", Ports.free());
            try (TcpProxy proxy = new TcpProxy(ANY, dead).start();
                 Socket client = connect(proxy)) {
                assertTrue(ended(client));
                await(() -> proxy.stats().active() == 0, 5_000);
                assertEquals(1, proxy.stats().accepted());
            }
        });
    }

    @Test void closeTerminatesLiveConnections() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            try (TestServer server = new TestServer(TcpProxyTest::echo)) {
                TcpProxy proxy = new TcpProxy(ANY, server.address()).start();
                try (Socket client = connect(proxy)) {
                    client.getOutputStream().write(9);
                    assertEquals(9, client.getInputStream().read());
                    proxy.close();
                    assertTrue(ended(client));
                }
                assertEquals(0, proxy.stats().active());
            }
        });
    }

    @Test void lifecycleIsChecked() throws Exception {
        TcpProxy proxy = new TcpProxy(ANY, HostPort.of("127.0.0.1", 9));
        assertThrows(IllegalStateException.class, proxy::port);
        proxy.start();
        assertTrue(proxy.port() > 0);
        assertThrows(IllegalStateException.class, proxy::start);
        proxy.close();
        proxy.close();
        assertThrows(IllegalStateException.class, proxy::start);
        TcpProxy never = new TcpProxy(ANY, HostPort.of("127.0.0.1", 9));
        never.close();
        assertThrows(IllegalStateException.class, never::start);
    }

    @Test void optionsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> TcpProxy.Options.defaults().withMaxConnections(0));
        assertThrows(IllegalArgumentException.class, () -> TcpProxy.Options.defaults().withConnectTimeoutMillis(0));
        assertThrows(IllegalArgumentException.class, () -> TcpProxy.Options.defaults().withIdleTimeoutMillis(-1));
        assertThrows(IllegalArgumentException.class, () -> TcpProxy.Options.defaults().withBufferSize(0));
        assertThrows(NullPointerException.class, () -> new TcpProxy(null, ANY));
        assertThrows(NullPointerException.class, () -> new TcpProxy(ANY, null));
    }
}
