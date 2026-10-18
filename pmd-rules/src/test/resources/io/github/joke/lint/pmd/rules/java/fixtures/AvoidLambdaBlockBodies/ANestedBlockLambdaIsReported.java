public class Example {
    void m(java.util.List<java.util.List<String>> nested) {
        nested.forEach(inner -> inner.forEach(item -> {
            validate(item);
            save(item);
        }));
    }

    void validate(String s) { }
    void save(String s) { }
}
