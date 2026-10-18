public class Example {
    void m(java.util.List<String> items) {
        items.forEach(item -> {
            save(item);
        });
    }

    void save(String s) { }
}
