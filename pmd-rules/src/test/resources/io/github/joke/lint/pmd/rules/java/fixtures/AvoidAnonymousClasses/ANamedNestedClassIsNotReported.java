public class Example {
    Runnable m() {
        return new Worker();
    }

    static final class Worker implements Runnable {
        @Override
        public void run() { }
    }
}
