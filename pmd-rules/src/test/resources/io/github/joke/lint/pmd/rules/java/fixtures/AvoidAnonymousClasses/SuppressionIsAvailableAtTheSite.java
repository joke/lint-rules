public class Example {
    @SuppressWarnings("PMD.AvoidAnonymousClasses")
    Runnable m() {
        return new Runnable() {
            @Override
            public void run() { }
        };
    }
}
