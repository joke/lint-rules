public class Example {
    java.util.function.Function<java.util.List<String>, Long> m() {
        return list -> list.stream().filter(java.util.Objects::nonNull).count();
    }
}
