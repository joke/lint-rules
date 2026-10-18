public class Example {
    private static String instance;

    static void reset() { instance = null; }
    String keep() { return "x"; }
}
