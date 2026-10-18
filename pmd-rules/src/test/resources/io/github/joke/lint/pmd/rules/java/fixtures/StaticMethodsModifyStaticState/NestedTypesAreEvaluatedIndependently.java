public class Example {
    String keep() { return "x"; }

    static final class Nested {
        private Nested() { }

        static String format(int n) { return "" + n; }
    }
}
