import lombok.experimental.UtilityClass;

@UtilityClass
public class Coercion {
    static String format(int n) { return "" + n; }

    String render(int n) { return "" + n; }
}
