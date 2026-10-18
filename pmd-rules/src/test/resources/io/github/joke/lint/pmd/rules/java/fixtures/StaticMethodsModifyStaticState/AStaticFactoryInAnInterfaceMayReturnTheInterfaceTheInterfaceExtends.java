public interface Special extends Foo {
    static Foo of(int x) { return null; }

    default String keep() { return "x"; }
}
