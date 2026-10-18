public class Foo<T> {
    static <T> Foo<T> of(T x) { return new Foo<T>(); }

    String keep() { return "x"; }
}
