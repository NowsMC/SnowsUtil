package space.nows.util.net;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class HostPortOddTest {
    @Test
    void parseAcceptsPortBoundaries() {
        assertEquals(new HostPort("h", 0), HostPort.parse("h:0"));
        assertEquals(new HostPort("h", 65535), HostPort.parse("h:65535"));
    }

    @Test
    void parseRejectsBadPorts() {
        for (String bad : new String[]{"h:65536", "h:-1", "h:+80", "h:", "h:80x", "h:99999999999", "h"}) {
            assertThrows(IllegalArgumentException.class, () -> HostPort.parse(bad), bad);
        }
    }

    @Test
    void unbracketedIpv6IsRejectedAndBracketedAccepted() {
        assertThrows(IllegalArgumentException.class, () -> HostPort.parse("::1:80"));
        assertEquals(new HostPort("::1", 80), HostPort.parse("[::1]:80"));
        assertThrows(IllegalArgumentException.class, () -> HostPort.parse("[::1]80"));
        assertThrows(IllegalArgumentException.class, () -> HostPort.parse("[::1"));
    }

    @Test
    void toStringRoundTripsThroughParse() {
        String[] samples = {"example.com:443", "[2001:db8::1]:8080", "127.0.0.1:1"};
        for (String s : samples) assertEquals(s, HostPort.parse(s).toString(), s);
    }

    @Test
    void constructorRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> HostPort.of("h", -1));
        assertThrows(IllegalArgumentException.class, () -> HostPort.of("h", 65536));
        assertThrows(RuntimeException.class, () -> HostPort.of(null, 80));
    }

    @Test
    void portsFreeReturnsAUsableEphemeralPort() throws IOException {
        int port = space.nows.util.net.Ports.free();
        assertTrue(port > 0 && port <= 65535);
        assertFalse(space.nows.util.net.Ports.isOpen(HostPort.of("127.0.0.1", port), 200));
    }

    @Test
    void portsIsOpenDetectsAListeningSocket() throws IOException {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            assertTrue(Ports.isOpen(HostPort.of(InetAddress.getLoopbackAddress().getHostAddress(), server.getLocalPort()), 1000));
        }
    }
}
