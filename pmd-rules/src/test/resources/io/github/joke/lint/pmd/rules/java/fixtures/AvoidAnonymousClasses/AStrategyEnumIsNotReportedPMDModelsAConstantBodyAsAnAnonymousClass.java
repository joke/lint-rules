public enum Op {
    PLUS {
        @Override
        int apply(int a, int b) { return a + b; }
    },
    MINUS {
        @Override
        int apply(int a, int b) { return a - b; }
    };

    abstract int apply(int a, int b);
}
