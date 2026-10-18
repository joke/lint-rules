public class Example {
    private static final java.util.Map<String, String> REGISTRY = new java.util.HashMap<>();

    static void register(String k, String v) { REGISTRY.put(k, v); }
    String keep() { return "x"; }
}
