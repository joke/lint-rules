public class FooImpl implements Foo {
    static Foo of(int x) { return new FooImpl(); }

    String keep() { return "x"; }
}
