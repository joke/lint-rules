public class Foo extends Base {
    static Base of(int x) { return new Foo(); }

    String keep() { return "x"; }
}
