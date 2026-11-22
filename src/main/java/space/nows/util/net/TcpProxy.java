package space.nows.util.net;

import space.nows.util.check.Preconditions;
import space.nows.util.concurrent.NamedThreadFactory;
import space.nows.util.io.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

/**
 * A small TCP forwarding proxy: every connection accepted on the listen address is relayed to the target, byte for
 * byte, in both directions. It uses two threads per connection, so it suits tens to hundreds of connections rather
 * than tens of thousands. Half-closed connections are relayed correctly: when one side finishes sending, the other
 * side sees end of stream while its own data keeps flowing.
 *
 * <p>The target host is resolved for each connection. Bind to a loopback address unless the proxy is meant to be
 * reachable from other machines; it performs no authentication.
 */
public final class TcpProxy implements AutoCloseable {
    /** Connection limits and timeouts. An idle timeout of 0 disables idle disconnects. */
    public record Options(int maxConnections, int connectTimeoutMillis, int idleTimeoutMillis, int bufferSize) {
        public Options {
            Preconditions.positive(maxConnections, "maxConnections");
            Preconditions.positive(connectTimeoutMillis, "connectTimeoutMillis");
            Preconditions.nonNegative(idleTimeoutMillis, "idleTimeoutMillis");
            Preconditions.positive(bufferSize, "bufferSize");
        }

        public static Options defaults() { return new Options(256, 5_000, 0, 16 * 1024); }

        public Options withMaxConnections(int value) { return new Options(value, connectTimeoutMillis, idleTimeoutMillis, bufferSize); }

        public Options withConnectTimeoutMillis(int value) { return new Options(maxConnections, value, idleTimeoutMillis, bufferSize); }

        /** A connection is closed after this long with no data moving in either direction. */
        public Options withIdleTimeoutMillis(int value) { return new Options(maxConnections, connectTimeoutMillis, value, bufferSize); }

        public Options withBufferSize(int value) { return new Options(maxConnections, connectTimeoutMillis, idleTimeoutMillis, value); }
    }

    /** A snapshot of the proxy's counters. */
    public record Stats(long accepted, long rejected, int active, long bytesToTarget, long bytesFromTarget) {}

    private final HostPort listen;
    private final HostPort target;
    private final Options options;
    private final Set<Connection> connections = ConcurrentHashMap.newKeySet();
    private final LongAdder accepted = new LongAdder();
    private final LongAdder rejected = new LongAdder();
    private final LongAdder toTarget = new LongAdder();
    private final LongAdder fromTarget = new LongAdder();
    private final AtomicInteger active = new AtomicInteger();
    private ServerSocket server;
    private ExecutorService executor;
    private Thread acceptor;
    private volatile boolean closed;

    public TcpProxy(HostPort listen, HostPort target) { this(listen, target, Options.defaults()); }

    public TcpProxy(HostPort listen, HostPort target, Options options) {
        this.listen = Preconditions.notNull(listen, "listen");
        this.target = Preconditions.notNull(target, "target");
        this.options = Preconditions.notNull(options, "options");
    }

    /** Binds the listen address and starts accepting; returns this proxy. A listen port of 0 picks a free port. */
    public synchronized TcpProxy start() throws IOException {
        if (closed) throw new IllegalStateException("proxy is closed");
        if (server != null) throw new IllegalStateException("proxy is already started");
        ServerSocket s = new ServerSocket();
        try {
            s.bind(listen.resolve(), 128);
        } catch (IOException | RuntimeException e) {
            Streams.closeQuietly(s);
            throw e;
        }
        server = s;
        String name = "tcp-proxy-" + s.getLocalPort();
        executor = Executors.newCachedThreadPool(new NamedThreadFactory(name));
        acceptor = new Thread(this::acceptLoop, name + "-accept");
        acceptor.setDaemon(true);
        acceptor.start();
        return this;
    }

    /** The port actually bound, which differs from the requested one when that was 0. */
    public synchronized int port() {
        if (server == null) throw new IllegalStateException("proxy is not started");
        return server.getLocalPort();
    }

    public Stats stats() {
        return new Stats(accepted.sum(), rejected.sum(), active.get(), toTarget.sum(), fromTarget.sum());
    }

    /** Stops accepting, closes every open connection and releases the threads. Safe to call more than once. */
    @Override public void close() {
        ServerSocket s;
        ExecutorService e;
        synchronized (this) {
            if (closed) return;
            closed = true;
            s = server;
            e = executor;
        }
        Streams.closeQuietly(s);
        for (Connection c : connections) c.abort();
        if (e != null) {
            e.shutdownNow();
            try {
                e.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void acceptLoop() {
        while (!closed) {
            Socket client;
            try {
                client = server.accept();
            } catch (IOException e) {
                if (closed || server.isClosed()) return;
                LockSupport.parkNanos(10_000_000L);
                continue;
            }
            if (active.get() >= options.maxConnections()) {
                rejected.increment();
                Streams.closeQuietly(client);
                continue;
            }
            accepted.increment();
            active.incrementAndGet();
            Connection c = new Connection(client);
            connections.add(c);
            try {
                executor.execute(c::run);
            } catch (RejectedExecutionException e) {
                c.finish();
            }
        }
    }

    private final class Connection {
        private final Socket client;
        private final Socket upstream = new Socket();
        private final long idleNanos = TimeUnit.MILLISECONDS.toNanos(options.idleTimeoutMillis());
        private volatile boolean aborted;
        private volatile long lastActivity = System.nanoTime();

        Connection(Socket client) { this.client = client; }

        void run() {
            try {
                upstream.connect(target.resolve(), options.connectTimeoutMillis());
                if (aborted) throw new IOException("proxy closed");
                client.setTcpNoDelay(true);
                upstream.setTcpNoDelay(true);
                if (idleNanos > 0) {
                    int poll = Math.min(options.idleTimeoutMillis(), 1_000);
                    client.setSoTimeout(poll);
                    upstream.setSoTimeout(poll);
                }
                CountDownLatch done = new CountDownLatch(1);
                executor.execute(() -> {
                    try {
                        pump(client, upstream, toTarget);
                    } finally {
                        done.countDown();
                    }
                });
                pump(upstream, client, fromTarget);
                done.await();
            } catch (IOException | RejectedExecutionException e) {
                abort();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                abort();
            } finally {
                finish();
            }
        }

        private void pump(Socket from, Socket to, LongAdder counter) {
            byte[] buffer = new byte[options.bufferSize()];
            try {
                InputStream in = from.getInputStream();
                OutputStream out = to.getOutputStream();
                for (;;) {
                    int n;
                    try {
                        n = in.read(buffer);
                    } catch (SocketTimeoutException e) {
                        if (System.nanoTime() - lastActivity >= idleNanos) throw e;
                        continue;
                    }
                    if (n < 0) break;
                    out.write(buffer, 0, n);
                    counter.add(n);
                    lastActivity = System.nanoTime();
                }
                try {
                    to.shutdownOutput();
                } catch (IOException ignored) {
                    abort();
                }
            } catch (IOException e) {
                abort();
            }
        }

        void abort() {
            aborted = true;
            Streams.closeQuietly(client);
            Streams.closeQuietly(upstream);
        }

        void finish() {
            Streams.closeQuietly(client);
            Streams.closeQuietly(upstream);
            if (connections.remove(this)) active.decrementAndGet();
        }
    }
}
