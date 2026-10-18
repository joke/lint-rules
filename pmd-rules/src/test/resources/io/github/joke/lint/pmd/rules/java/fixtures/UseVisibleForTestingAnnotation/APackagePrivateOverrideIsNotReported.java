class Base {
    @VisibleForTesting
    void hook() { }
}

public class Example extends Base {
    @Override
    void hook() { }
}
