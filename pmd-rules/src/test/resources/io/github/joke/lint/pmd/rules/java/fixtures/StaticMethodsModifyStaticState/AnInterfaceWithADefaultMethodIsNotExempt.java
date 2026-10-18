public interface Example {
    static String format(int n) { return "" + n; }

    default String keep() { return "x"; }
}
