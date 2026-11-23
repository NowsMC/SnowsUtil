package space.nows.util.data.mapping;

import org.junit.jupiter.api.Test;
import space.nows.util.data.Str;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;
import space.nows.util.json.Json;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MapperFeaturesTest {
    record Account(String firstName, String userID, int balanceCents) {}

    static class Credentials {
        @Required String token;
        String note;
    }

    static final class Money {
        final String currency;
        final long cents;

        @Creator
        Money(@Property("currency") String currency, @Property("cents") long cents) {
            this.currency = currency;
            this.cents = cents;
        }
    }

    static final class BrokenCreator {
        final int x;

        @Creator
        BrokenCreator(int x) {
            this.x = x;
        }
    }

    @TypeInfo(property = "kind", subtypes = {
            @TypeInfo.SubType(name = "dog", type = Dog.class),
            @TypeInfo.SubType(name = "cat", type = Cat.class)
    })
    static abstract class Animal {
        String name;
    }

    static final class Dog extends Animal {
        boolean goodBoy;
    }

    static final class Cat extends Animal {
        int lives;
    }

    static final class Owner {
        Animal pet;
        List<Animal> pets;
    }

    enum Level { LOW, HIGH }

    @Test
    void snakeCaseAndKebabAndUpperNamingApplyWithoutExplicitProperties() {
        Mapper snake = new Mapper().naming(Naming.SNAKE_CASE);
        assertEquals("{\"first_name\":\"Ann\",\"user_id\":\"u1\",\"balance_cents\":5}",
                Json.write(snake.toData(new Account("Ann", "u1", 5))));
        Mapper kebab = new Mapper().naming(Naming.KEBAB_CASE);
        assertEquals("first-name", Json.write(kebab.toData(new Account("Ann", "u", 0))).split("\"")[1]);
        Mapper upper = new Mapper().naming(Naming.UPPER_SNAKE_CASE);
        assertEquals("FIRST_NAME", Json.write(upper.toData(new Account("Ann", "u", 0))).split("\"")[1]);
        Account back = snake.fromData(Json.parse("{\"first_name\":\"Bo\",\"user_id\":\"u2\",\"balance_cents\":9}"), Account.class);
        assertEquals(new Account("Bo", "u2", 9), back);
    }

    @Test
    void namingNeverOverridesAnExplicitPropertyName() {
        record Explicit(@Property("kept_as_is") String someField) {}
        Mapper snake = new Mapper().naming(Naming.SNAKE_CASE);
        assertEquals("{\"kept_as_is\":\"x\"}", Json.write(snake.toData(new Explicit("x"))));
    }

    @Test
    void namingSplitsAcronymsAndDigits() {
        assertEquals("url_path", Naming.SNAKE_CASE.apply("URLPath"));
        assertEquals("user_id", Naming.SNAKE_CASE.apply("userID"));
        assertEquals("v2_api", Naming.SNAKE_CASE.apply("v2Api"));
        assertEquals("already_snake", Naming.SNAKE_CASE.apply("already_snake"));
        assertEquals("", Naming.SNAKE_CASE.apply(""));
    }

    @Test
    void requiredPropertyMustBePresentAndNonNull() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{}"), Credentials.class));
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"token\":null}"), Credentials.class));
        Credentials ok = mapper.fromData(Json.parse("{\"token\":\"t\"}"), Credentials.class);
        assertEquals("t", ok.token);
        assertNull(ok.note, "optional properties keep their default");
    }

    @Test
    void creatorConstructorBindsImmutableClasses() {
        Mapper mapper = new Mapper();
        Money money = mapper.fromData(Json.parse("{\"currency\":\"THB\",\"cents\":250}"), Money.class);
        assertEquals("THB", money.currency);
        assertEquals(250L, money.cents);
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"x\":1}"), BrokenCreator.class),
                "creator parameters need @Property");
    }

    @Test
    void standardTypesRoundTrip() {
        Mapper mapper = new Mapper();
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertEquals(id, mapper.fromData(mapper.toData(id), UUID.class));
        URI uri = URI.create("https://example.com/a?b=c");
        assertEquals(uri, mapper.fromData(mapper.toData(uri), URI.class));
        java.io.File file = new java.io.File("some/file.txt");
        assertEquals(file, mapper.fromData(mapper.toData(file), java.io.File.class));
        Path path = Path.of("some", "dir", "file.txt");
        assertEquals(path, mapper.fromData(mapper.toData(path), Path.class));
        assertEquals(Currency.getInstance("THB"), mapper.fromData(mapper.toData(Currency.getInstance("THB")), Currency.class));
        assertEquals(Locale.forLanguageTag("th-TH"), mapper.fromData(mapper.toData(Locale.forLanguageTag("th-TH")), Locale.class));
        Instant now = Instant.parse("2026-11-23T10:15:30.123456789Z");
        assertEquals(now, mapper.fromData(mapper.toData(now), Instant.class));
        assertEquals(LocalDate.of(2026, 11, 23), mapper.fromData(mapper.toData(LocalDate.of(2026, 11, 23)), LocalDate.class));
        assertEquals(Duration.ofMinutes(90), mapper.fromData(mapper.toData(Duration.ofMinutes(90)), Duration.class));
        OffsetDateTime offset = OffsetDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.ofHours(7));
        assertEquals(offset, mapper.fromData(mapper.toData(offset), OffsetDateTime.class));
        ZonedDateTime zoned = ZonedDateTime.of(2026, 7, 1, 12, 0, 0, 0, java.time.ZoneId.of("Asia/Bangkok"));
        assertEquals(zoned.toInstant(), mapper.fromData(mapper.toData(zoned), ZonedDateTime.class).toInstant());
    }

    @Test
    void malformedStandardTextIsADataException() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(new Str("not-a-uuid"), UUID.class));
        assertThrows(DataException.class, () -> mapper.fromData(new Str("2026-13-40"), LocalDate.class));
    }

    @Test
    void customAdapterReplacesTheReflectiveMapping() {
        record Point(int x, int y) {}
        Mapper mapper = new Mapper().register(Point.class, new Adapter<Point>() {
            @Override public Value write(Point p) { return new Str(p.x() + "," + p.y()); }
            @Override public Point read(Value v) {
                String[] parts = v.asString().split(",");
                return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            }
        });
        assertEquals("\"3,4\"", Json.write(mapper.toData(new Point(3, 4))));
        assertEquals(new Point(5, 6), mapper.fromData(Json.parse("\"5,6\""), Point.class));
    }

    @Test
    void polymorphicValuesWriteAndReadTheirTypeTag() {
        Mapper mapper = new Mapper();
        Dog dog = new Dog();
        dog.name = "Rex";
        dog.goodBoy = true;
        String json = Json.write(mapper.toData(dog));
        assertEquals("{\"kind\":\"dog\",\"name\":\"Rex\",\"goodBoy\":true}", json);
        Animal back = mapper.fromData(Json.parse(json), Animal.class);
        assertInstanceOf(Dog.class, back);
        assertTrue(((Dog) back).goodBoy);
        Dog direct = mapper.fromData(Json.parse(json), Dog.class);
        assertEquals("Rex", direct.name, "a subtype decoded directly accepts its own tag");
    }

    @Test
    void polymorphicErrorsAreClear() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"name\":\"x\"}"), Animal.class), "missing tag");
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"kind\":\"bird\"}"), Animal.class), "unknown tag");
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"kind\":\"cat\",\"bogus\":1}"), Animal.class),
                "strict mode still rejects unknown properties");
    }

    @Test
    void polymorphicFieldsInsideOtherTypes() {
        Mapper mapper = new Mapper();
        Owner owner = new Owner();
        Cat cat = new Cat();
        cat.name = "Mo";
        cat.lives = 9;
        owner.pet = cat;
        owner.pets = List.of(cat);
        Owner back = mapper.fromData(mapper.toData(owner), Owner.class);
        assertInstanceOf(Cat.class, back.pet);
        assertEquals(9, ((Cat) back.pet).lives);
        assertInstanceOf(Cat.class, back.pets.get(0));
    }

    @Test
    void enumConstantNamesStillWorkWithNaming() {
        Mapper mapper = new Mapper().naming(Naming.SNAKE_CASE);
        assertEquals("\"HIGH\"", Json.write(mapper.toData(Level.HIGH)));
    }
}
