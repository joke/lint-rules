public enum Op {
    PLUS;

    Runnable make() {
        return new Runnable() {
            @Override
            public void run() { }
        };
    }
}
