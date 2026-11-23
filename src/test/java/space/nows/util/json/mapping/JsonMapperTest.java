package space.nows.util.json.mapping;

import org.junit.jupiter.api.Test;
import space.nows.util.error.JsonException;
import space.nows.util.json.Json;
import space.nows.util.json.JsonNull;
import space.nows.util.json.JsonValue;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class JsonMapperTest {
    record Person(@JsonProperty("full_name") String name, int age, List<String> tags) {}

    record Wrapper(Optional<String> nickname, Map<String, Integer> scores, int[] counts) {}

    static class Holder {
        List<Person> people;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Profile {
        String name;
        @JsonIgnore String secret;
    }

    enum Role {
        @JsonProperty("admin") ADMIN,
        USER
    }

    static class Base {
        String id;
    }

    static class Account extends Base {
        int balance;
    }

    @Test
    void roundTripsRecordsWithRenamedProperties() {
        JsonMapper mapper = new JsonMapper();
        Person person = mapper.fromJson("{\"full_name\":\"Mina\",\"age\":27,\"tags\":[\"json\",\"java\"]}", Person.class);

        assertEquals(new Person("Mina", 27, List.of("json", "java")), person);
        assertEquals("{\"full_name\":\"Mina\",\"age\":27,\"tags\":[\"json\",\"java\"]}", mapper.toJson(person));
        assertEquals(person, mapper.fromJson(mapper.toJson(person), Person.class));
    }

    @Test
    void bindsGenericTypes() throws Exception {
        JsonMapper mapper = new JsonMapper();
        Type type = Holder.class.getDeclaredField("people").getGenericType();
        List<Person> people = mapper.fromJson(Json.parse("""
                [{"full_name":"A","age":1,"tags":[]},{"full_name":"B","age":2,"tags":["x"]}]
                """), type);

        assertEquals(List.of("A", "B"), people.stream().map(Person::name).toList());
    }

    @Test
    void mapsPlainTypesWithoutTargetClass() {
        JsonMapper mapper = new JsonMapper();

        @SuppressWarnings("unchecked")
        Map<String, Object> plain = mapper.fromJson(Json.parse("{\"n\":1,\"nested\":[true,null]}"), Map.class);
        assertEquals(1, plain.get("n"));
        assertInstanceOf(List.class, plain.get("nested"));
        assertEquals(1, mapper.fromJson(Json.parse("1"), Object.class));
        assertSame(JsonNull.INSTANCE, mapper.fromJson(JsonNull.INSTANCE, JsonValue.class));

        Collection<?> values = mapper.fromJson(Json.parse("[1,2]"), Collection.class);
        assertEquals(List.of(1, 2), List.copyOf(values));
        assertThrows(JsonException.class, () -> mapper.fromJson(Json.parse("[1]"), Queue.class));
    }

    @Test
    void bindsOptionalMapsAndArrays() {
        JsonMapper mapper = new JsonMapper();
        Wrapper wrapper = mapper.fromJson("{\"nickname\":null,\"scores\":{\"a\":1},\"counts\":[2,3]}", Wrapper.class);

        assertEquals(Optional.empty(), wrapper.nickname());
        assertEquals(Map.of("a", 1), wrapper.scores());
        assertArrayEquals(new int[]{2, 3}, wrapper.counts());
        assertEquals("{\"nickname\":null,\"scores\":{\"a\":1},\"counts\":[2,3]}", mapper.toJson(wrapper));
    }

    @Test
    void omitsNullsAndIgnoresMarkedFields() {
        JsonMapper mapper = new JsonMapper();
        Profile profile = mapper.fromJson("{\"name\":\"Kai\",\"secret\":\"inbound\"}", Profile.class);

        assertEquals("Kai", profile.name);
        assertNull(profile.secret);
        assertEquals("{\"name\":\"Kai\"}", mapper.toJson(profile));
    }

    @Test
    void unknownPropertyPolicy() {
        String json = "{\"name\":\"Kai\",\"extra\":1}";

        assertThrows(JsonException.class, () -> new JsonMapper().fromJson(json, Profile.class));
        assertEquals("Kai", JsonMapper.lenient().fromJson(json, Profile.class).name);
    }

    @Test
    void enumsUseAnnotatedNamesAndFallBackToConstantNames() {
        JsonMapper mapper = new JsonMapper();

        assertEquals("admin", mapper.toJsonValue(Role.ADMIN).asString());
        assertEquals("USER", mapper.toJsonValue(Role.USER).asString());
        assertEquals(Role.ADMIN, mapper.fromJson("\"admin\"", Role.class));
        assertEquals(Role.ADMIN, mapper.fromJson("\"ADMIN\"", Role.class));
        assertThrows(JsonException.class, () -> mapper.fromJson("\"GUEST\"", Role.class));
    }

    @Test
    void includesInheritedFieldsBaseFirst() {
        JsonMapper mapper = new JsonMapper();
        Account account = mapper.fromJson("{\"id\":\"a1\",\"balance\":5}", Account.class);

        assertEquals("a1", account.id);
        assertEquals(5, account.balance);
        assertEquals("{\"id\":\"a1\",\"balance\":5}", mapper.toJson(account));
    }

    @Test
    void reportsBindingFailures() {
        JsonMapper mapper = new JsonMapper();

        assertThrows(JsonException.class, () -> mapper.fromJson("{\"full_name\":\"A\",\"age\":1.5,\"tags\":[]}", Person.class));
        assertThrows(JsonException.class, () -> mapper.toJson(Double.NaN));
    }
}
