package space.nows.util.data.mapping;

import space.nows.util.data.Str;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Mapper configuration shared by encoding and decoding. */
final class Settings {
    private final boolean failOnUnknown;
    private final Map<Class<?>, Adapter<?>> adapters = new HashMap<>();
    private Naming naming = Naming.IDENTITY;

    Settings(boolean failOnUnknown) {
        this.failOnUnknown = failOnUnknown;
        standard(UUID.class, UUID::toString, UUID::fromString);
        standard(URI.class, URI::toString, URI::create);
        standard(Path.class, Path::toString, Path::of);
        standard(File.class, File::getPath, File::new);
        standard(Currency.class, Currency::getCurrencyCode, Currency::getInstance);
        standard(Locale.class, Locale::toLanguageTag, Locale::forLanguageTag);
        standard(Charset.class, Charset::name, Charset::forName);
        standard(Instant.class, Instant::toString, Instant::parse);
        standard(LocalDate.class, LocalDate::toString, LocalDate::parse);
        standard(LocalTime.class, LocalTime::toString, LocalTime::parse);
        standard(LocalDateTime.class, LocalDateTime::toString, LocalDateTime::parse);
        standard(OffsetDateTime.class, OffsetDateTime::toString, OffsetDateTime::parse);
        standard(ZonedDateTime.class, ZonedDateTime::toString, ZonedDateTime::parse);
        standard(Duration.class, Duration::toString, Duration::parse);
        standard(Period.class, Period::toString, Period::parse);
    }

    boolean failOnUnknown() { return failOnUnknown; }

    Naming naming() { return naming; }

    void naming(Naming value) { naming = value; }

    Map<Class<?>, Adapter<?>> adapters() { return adapters; }

    /** Explicit {@link Property} name if given, otherwise the member name under the naming strategy. */
    String propertyName(String memberName, Property property) {
        if (property != null && !property.value().isEmpty()) return property.value();
        return naming.apply(memberName);
    }

    private <T> void standard(Class<T> type, Function<T, String> write, Function<String, T> read) {
        adapters.put(type, new Adapter<T>() {
            @Override public Value write(T value) {
                return new Str(write.apply(value));
            }

            @Override public T read(Value value) {
                String text = value.asString();
                try {
                    return read.apply(text);
                } catch (RuntimeException e) {
                    throw new DataException("cannot read " + type.getSimpleName() + " from \"" + text + "\"", e);
                }
            }
        });
    }
}
