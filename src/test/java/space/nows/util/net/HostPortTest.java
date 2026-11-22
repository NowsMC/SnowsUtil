package space.nows.util.net;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class HostPortTest {
    @Test void parsesHostNamesAndAddresses() {
        assertEquals(new HostPort("example.com", 80), HostPort.parse("example.com:80"));
        assertEquals(new HostPort("127.0.0.1", 0), HostPort.parse("127.0.0.1:0"));
        assertEquals(new HostPort("localhost", 65535), HostPort.parse("localhost:65535"));
        assertEquals(new HostPort("::1", 8080), HostPort.parse("[::1]:8080"));
        assertEquals(new HostPort("fe80::1%eth0", 9), HostPort.parse("[fe80::1%eth0]:9"));
    }

    @Test void formatsAndRoundTrips() {
        for (String text : new String[] {"example.com:80", "10.0.0.1:1", "[::1]:8080", "[2001:db8::2]:443"}) {
            assertEquals(text, HostPort.parse(text).toString());
        }
        assertEquals("a:5", HostPort.of("a", 5).toString());
    }

    @Test void rejectsMalformedInput() {
        for (String text : new String[] {"", "host", "host:", ":80", "host:-1", "host:+80", "host:65536", "host:8o", "::1:80",
                "[::1]", "[::1]80", "[::1", "[]:80", "host:99999999999", "host: 80"}) {
            assertThrows(IllegalArgumentException.class, () -> HostPort.parse(text), text);
        }
        assertThrows(NullPointerException.class, () -> HostPort.parse(null));
        assertThrows(IllegalArgumentException.class, () -> HostPort.of("host", 70000));
        assertThrows(IllegalArgumentException.class, () -> HostPort.of("", 80));
    }

    @Test void resolveKeepsThePort() {
        assertEquals(8080, HostPort.parse("127.0.0.1:8080").resolve().getPort());
        assertTrue(HostPort.of("no-such-host.invalid", 80).resolve().isUnresolved());
    }

    @Test void portsHelpers() throws IOException {
        int port = Ports.free();
        assertTrue(port > 0 && port < 65536);
        assertFalse(Ports.isOpen(HostPort.of("127.0.0.1", port), 500));
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            assertTrue(Ports.isOpen(HostPort.of("127.0.0.1", server.getLocalPort()), 2_000));
        }
        assertThrows(IllegalArgumentException.class, () -> Ports.isOpen(HostPort.of("127.0.0.1", 1), 0));
        assertThrows(NullPointerException.class, () -> Ports.isOpen(null, 100));
    }
}
