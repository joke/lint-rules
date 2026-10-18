public class FooImpl implements Foo {
    static Base of(int x) { return new FooImpl(); }

    String keep() { return "x"; }
}
