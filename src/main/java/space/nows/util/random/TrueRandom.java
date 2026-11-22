package space.nows.util.random;

import space.nows.util.check.Preconditions;
import space.nows.util.text.Hex;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Non-deterministic, cryptographically secure random numbers from the operating system's entropy source, through
 * {@link SecureRandom}. Java has no portable access to a hardware random number generator, so this is the
 * strongest randomness available without extra dependencies. All {@link RandomGenerator} methods are inherited
 * and unbiased. Instances are thread-safe; calls on one instance may contend, so give hot threads their own.
 */
public final class TrueRandom implements RandomGenerator {
    private static final class Holder {
        static final TrueRandom SHARED = new TrueRandom(new SecureRandom());
    }

    private final SecureRandom source;

    private TrueRandom(SecureRandom source) { this.source = source; }

    /** The process-wide instance backed by the platform's default {@link SecureRandom}; never blocks for entropy. */
    public static TrueRandom shared() { return Holder.SHARED; }

    /** A new instance with its own default {@link SecureRandom}, so it does not contend with {@link #shared()}. */
    public static TrueRandom create() { return new TrueRandom(new SecureRandom()); }

    /** A new instance backed by {@link SecureRandom#getInstanceStrong()}, which may block until enough entropy is available. */
    public static TrueRandom strong() {
        try {
            return new TrueRandom(SecureRandom.getInstanceStrong());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("no strong SecureRandom algorithm is available", e);
        }
    }

    public static TrueRandom of(SecureRandom source) { return new TrueRandom(Preconditions.notNull(source, "source")); }

    @Override public long nextLong() { return source.nextLong(); }

    @Override public int nextInt() { return source.nextInt(); }

    @Override public void nextBytes(byte[] bytes) { source.nextBytes(bytes); }

    /** A new array of {@code length} random bytes. */
    public byte[] bytes(int length) {
        byte[] out = new byte[Preconditions.nonNegative(length, "length")];
        source.nextBytes(out);
        return out;
    }

    /** Lower-case hex text of {@code byteCount} random bytes, for tokens and identifiers. */
    public String hex(int byteCount) { return Hex.toHex(bytes(byteCount)); }

    /** A random version 4 {@link UUID} drawn from this source. */
    public UUID uuid() {
        byte[] b = bytes(16);
        b[6] = (byte) ((b[6] & 0x0F) | 0x40);
        b[8] = (byte) ((b[8] & 0x3F) | 0x80);
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) msb = (msb << 8) | (b[i] & 0xFF);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (b[i] & 0xFF);
        return new UUID(msb, lsb);
    }

    /** {@code length} characters drawn uniformly, each independently, from {@code alphabet}. */
    public String string(int length, CharSequence alphabet) {
        Preconditions.nonNegative(length, "length");
        Preconditions.notEmpty(alphabet, "alphabet");
        char[] out = new char[length];
        int n = alphabet.length();
        for (int i = 0; i < length; i++) out[i] = alphabet.charAt(nextInt(n));
        return new String(out);
    }
}
