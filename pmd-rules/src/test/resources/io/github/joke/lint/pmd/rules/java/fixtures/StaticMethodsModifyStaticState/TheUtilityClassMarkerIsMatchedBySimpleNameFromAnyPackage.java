import com.example.annotations.UtilityClass;

@UtilityClass
public class Coercion {
    static String format(int n) { return "" + n; }

    String keep() { return "x"; }
}
