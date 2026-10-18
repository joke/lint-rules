public class Example {
    void m(java.util.List<String> items) {
        items.forEach(item -> {
            save(item);
        });
        items.forEach(item -> {
            validate(item);
        });
        items.forEach(item -> save(item));
        items.forEach(item -> { });
    }

    void validate(String s) { }
    void save(String s) { }
}
