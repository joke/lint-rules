public class Example {
    @SuppressWarnings("PMD.AvoidLambdaBlockBodies")
    void m(java.util.List<String> items) {
        items.forEach(item -> {
            validate(item);
            save(item);
        });
    }

    void validate(String s) { }
    void save(String s) { }
}
