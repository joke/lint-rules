public class Example {
    Runnable m() {
        return () -> {
            doTheWork();
        };
    }

    void doTheWork() { }
}
