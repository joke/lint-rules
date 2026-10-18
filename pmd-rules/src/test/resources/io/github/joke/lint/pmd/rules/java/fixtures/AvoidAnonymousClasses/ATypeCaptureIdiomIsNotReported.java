public class Example {
    Object m() {
        return new TypeToken<java.util.List<String>>() { };
    }

    static class TypeToken<T> { }
}
