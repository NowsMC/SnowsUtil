package space.nows.util.data.mapping;

/**
 * How Java member names become JSON property names when no {@link Property} gives one. Splits camelCase words:
 * {@code firstName} becomes {@code first_name}, and {@code userID} becomes {@code user_id}.
 */
public enum Naming {
    IDENTITY,
    SNAKE_CASE,
    KEBAB_CASE,
    UPPER_SNAKE_CASE;

    public String apply(String name) {
        return switch (this) {
            case IDENTITY -> name;
            case SNAKE_CASE -> words(name, '_', false);
            case KEBAB_CASE -> words(name, '-', false);
            case UPPER_SNAKE_CASE -> words(name, '_', true);
        };
    }

    private static String words(String name, char separator, boolean upper) {
        StringBuilder out = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                char before = name.charAt(i - 1);
                boolean afterLowerOrDigit = Character.isLowerCase(before) || Character.isDigit(before);
                boolean endOfAcronym = Character.isUpperCase(before) && i + 1 < name.length()
                        && Character.isLowerCase(name.charAt(i + 1));
                if ((afterLowerOrDigit || endOfAcronym) && out.charAt(out.length() - 1) != separator) {
                    out.append(separator);
                }
            }
            out.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
        }
        return out.toString();
    }
}
