package space.nows.util.json;

import java.util.Locale;

public enum JsonType {
    OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL;

    public String label() { return name().toLowerCase(Locale.ROOT); }
}
