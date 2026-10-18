public class Example {
    java.util.function.Function<String, String> m() {
        return new java.util.function.Function<String, String>() {
            @Override
            public String apply(String value) {
                return convert(value);
            }
        };
    }

    String convert(String value) { return value; }
}
