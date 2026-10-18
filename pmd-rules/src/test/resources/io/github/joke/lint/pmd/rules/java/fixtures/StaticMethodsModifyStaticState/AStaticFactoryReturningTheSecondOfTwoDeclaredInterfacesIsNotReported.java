public class FooImpl implements Serializable, Foo {
    static Foo of(int x) { return new FooImpl(); }

    String keep() { return "x"; }
}
