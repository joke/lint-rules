public class Example {
    protected void hook() { }

    static final class Sub extends Example {
        @Override
        protected void hook() { }
    }
}
