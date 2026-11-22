package space.nows.util.net;

import space.nows.util.check.Preconditions;
import space.nows.util.text.NumberParser;

import java.net.InetSocketAddress;

/**
 * A host name or IP address with a port. IPv6 literals are written in brackets, as in {@code [::1]:8080}. The host
 * is kept as text and only resolved by {@link #resolve()}, so a name is looked up again on every connection.
 */
public record HostPort(String host, int port) {
    public HostPort {
        Preconditions.notEmpty(host, "host");
        if (port < 0 || port > 65535) throw new IllegalArgumentException("port must be in [0, 65535] but was " + port);
    }

    public static HostPort of(String host, int port) { return new HostPort(host, port); }

    /**
     * Parses {@code host:port} or {@code [ipv6]:port}.
     *
     * @throws IllegalArgumentException if the text has no port, an invalid port, or an unbracketed IPv6 address
     */
    public static HostPort parse(String text) {
        Preconditions.notNull(text, "text");
        String host;
        String portText;
        if (text.startsWith("[")) {
            int close = text.indexOf(']');
            if (close < 0 || close + 1 >= text.length() || text.charAt(close + 1) != ':') {
                throw new IllegalArgumentException("expected [host]:port but was \"" + text + "\"");
            }
            host = text.substring(1, close);
            portText = text.substring(close + 2);
        } else {
            int colon = text.lastIndexOf(':');
            if (colon < 0) throw new IllegalArgumentException("expected host:port but was \"" + text + "\"");
            host = text.substring(0, colon);
            if (host.indexOf(':') >= 0) {
                throw new IllegalArgumentException("IPv6 addresses must be written in brackets: \"" + text + "\"");
            }
            portText = text.substring(colon + 1);
        }
        if (portText.isEmpty() || portText.charAt(0) < '0' || portText.charAt(0) > '9') {
            throw new IllegalArgumentException("invalid port in \"" + text + "\"");
        }
        int port = NumberParser.parseInt(portText, -1);
        if (port < 0) throw new IllegalArgumentException("invalid port in \"" + text + "\"");
        return new HostPort(host, port);
    }

    /** Resolves the host now; the result is unresolved if the name cannot be found. */
    public InetSocketAddress resolve() { return new InetSocketAddress(host, port); }

    @Override public String toString() {
        return host.indexOf(':') >= 0 ? "[" + host + "]:" + port : host + ":" + port;
    }
}
