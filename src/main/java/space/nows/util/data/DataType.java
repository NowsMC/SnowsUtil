package space.nows.util.data;

import java.util.Locale;

public enum DataType {
    OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL;

    public String label() { return name().toLowerCase(Locale.ROOT); }
}
