import lombok.experimental.UtilityClass;

@UtilityClass
public class Coercion {
    public static class Nested {
        static String format(int n) { return "" + n; }

        String keep() { return "x"; }
    }
}
