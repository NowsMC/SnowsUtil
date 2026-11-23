package space.nows.util.data.mapping;

import org.junit.jupiter.api.Test;
import space.nows.util.data.DataNull;
import space.nows.util.data.DataValue;
import space.nows.util.error.DataException;
import space.nows.util.json.Json;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class DataMapperTest {
    record Person(@DataProperty("full_name") String name, int age, List<String> tags) {}

    record Wrapper(Optional<String> nickname, Map<String, Integer> scores, int[] counts) {}

    static class Holder {
        List<Person> people;
    }

    @DataInclude(DataInclude.Include.NON_NULL)
    static class Profile {
        String name;
        @DataIgnore String secret;
    }

    enum Role {
        @DataProperty("admin") ADMIN,
        USER
    }

    static class Base {
        String id;
    }

    static class Account extends Base {
        int balance;
    }

    private static <T> T bind(String json, Class<T> type) {
        return new DataMapper().fromData(Json.parse(json), type);
    }

    private static String render(Object value) {
        return Json.write(new DataMapper().toData(value));
    }

    @Test
    void roundTripsRecordsWithRenamedProperties() {
        Person person = bind("{\"full_name\":\"Mina\",\"age\":27,\"tags\":[\"json\",\"java\"]}", Person.class);

        assertEquals(new Person("Mina", 27, List.of("json", "java")), person);
        assertEquals("{\"full_name\":\"Mina\",\"age\":27,\"tags\":[\"json\",\"java\"]}", render(person));
        assertEquals(person, bind(render(person), Person.class));
    }

    @Test
    void bindsGenericTypes() throws Exception {
        Type type = Holder.class.getDeclaredField("people").getGenericType();
        List<Person> people = new DataMapper().fromData(Json.parse("""
                [{"full_name":"A","age":1,"tags":[]},{"full_name":"B","age":2,"tags":["x"]}]
                """), type);

        assertEquals(List.of("A", "B"), people.stream().map(Person::name).toList());
    }

    @Test
    void mapsPlainTypesWithoutTargetClass() {
        DataMapper mapper = new DataMapper();

        @SuppressWarnings("unchecked")
        Map<String, Object> plain = mapper.fromData(Json.parse("{\"n\":1,\"nested\":[true,null]}"), Map.class);
        assertEquals(1, plain.get("n"));
        assertInstanceOf(List.class, plain.get("nested"));
        assertEquals(1, mapper.fromData(Json.parse("1"), Object.class));
        assertSame(DataNull.INSTANCE, mapper.fromData(DataNull.INSTANCE, DataValue.class));

        Collection<?> values = mapper.fromData(Json.parse("[1,2]"), Collection.class);
        assertEquals(List.of(1, 2), List.copyOf(values));
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("[1]"), Queue.class));
    }

    @Test
    void bindsOptionalMapsAndArrays() {
        Wrapper wrapper = bind("{\"nickname\":null,\"scores\":{\"a\":1},\"counts\":[2,3]}", Wrapper.class);

        assertEquals(Optional.empty(), wrapper.nickname());
        assertEquals(Map.of("a", 1), wrapper.scores());
        assertArrayEquals(new int[]{2, 3}, wrapper.counts());
        assertEquals("{\"nickname\":null,\"scores\":{\"a\":1},\"counts\":[2,3]}", render(wrapper));
    }

    @Test
    void omitsNullsAndIgnoresMarkedFields() {
        Profile profile = bind("{\"name\":\"Kai\",\"secret\":\"inbound\"}", Profile.class);

        assertEquals("Kai", profile.name);
        assertNull(profile.secret);
        assertEquals("{\"name\":\"Kai\"}", render(profile));
    }

    @Test
    void unknownPropertyPolicy() {
        String json = "{\"name\":\"Kai\",\"extra\":1}";

        assertThrows(DataException.class, () -> bind(json, Profile.class));
        assertEquals("Kai", DataMapper.lenient().fromData(Json.parse(json), Profile.class).name);
    }

    @Test
    void enumsUseAnnotatedNamesAndFallBackToConstantNames() {
        DataMapper mapper = new DataMapper();

        assertEquals("admin", mapper.toData(Role.ADMIN).asString());
        assertEquals("USER", mapper.toData(Role.USER).asString());
        assertEquals(Role.ADMIN, bind("\"admin\"", Role.class));
        assertEquals(Role.ADMIN, bind("\"ADMIN\"", Role.class));
        assertThrows(DataException.class, () -> bind("\"GUEST\"", Role.class));
    }

    @Test
    void includesInheritedFieldsBaseFirst() {
        Account account = bind("{\"id\":\"a1\",\"balance\":5}", Account.class);

        assertEquals("a1", account.id);
        assertEquals(5, account.balance);
        assertEquals("{\"id\":\"a1\",\"balance\":5}", render(account));
    }

    @Test
    void reportsBindingFailures() {
        assertThrows(DataException.class, () -> bind("{\"full_name\":\"A\",\"age\":1.5,\"tags\":[]}", Person.class));
        assertThrows(DataException.class, () -> new DataMapper().toData(Double.NaN));
    }
}
