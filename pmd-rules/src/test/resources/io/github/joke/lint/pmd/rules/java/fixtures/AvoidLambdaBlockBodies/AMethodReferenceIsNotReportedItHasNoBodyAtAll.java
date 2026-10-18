public class Example {
    void m(java.util.List<String> items) {
        items.forEach(this::save);
    }

    void save(String s) { }
}
