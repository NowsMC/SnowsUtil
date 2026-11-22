package space.nows.util.net;

import space.nows.util.check.Preconditions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/** Probing TCP ports. */
public final class Ports {
    private Ports() {}

    /**
     * A loopback TCP port that was free a moment ago. Another process can take it before you bind, so bind to
     * port 0 and read the port back where you can.
     */
    public static int free() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }

    /** Whether a TCP connection to {@code target} succeeds within {@code timeoutMillis}. */
    public static boolean isOpen(HostPort target, int timeoutMillis) {
        Preconditions.notNull(target, "target");
        Preconditions.positive(timeoutMillis, "timeoutMillis");
        try (Socket s = new Socket()) {
            s.connect(target.resolve(), timeoutMillis);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
