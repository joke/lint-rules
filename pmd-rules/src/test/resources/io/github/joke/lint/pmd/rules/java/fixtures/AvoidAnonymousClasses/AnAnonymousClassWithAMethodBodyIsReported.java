public class Example {
    Runnable m() {
        return new Runnable() {
            @Override
            public void run() {
                doTheWork();
            }
        };
    }

    void doTheWork() { }
}
