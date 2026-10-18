public class Example {
    void m(java.util.List<String> items, String context) {
        items.forEach(item -> process(item, context));
    }

    void process(String item, String context) { }
}
