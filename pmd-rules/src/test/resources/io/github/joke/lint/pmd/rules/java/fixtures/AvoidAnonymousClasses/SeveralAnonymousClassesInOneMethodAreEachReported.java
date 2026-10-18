public class Example {
    void m() {
        Runnable first = new Runnable() {
            @Override
            public void run() { }
        };
        Runnable second = new Runnable() {
            @Override
            public void run() { }
        };
        Runnable third = new Runnable() { };
    }
}
