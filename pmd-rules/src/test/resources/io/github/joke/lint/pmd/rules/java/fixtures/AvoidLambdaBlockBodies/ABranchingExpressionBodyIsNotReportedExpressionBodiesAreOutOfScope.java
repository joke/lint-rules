public class Example {
    java.util.function.Function<Integer, Integer> m() {
        return x -> x > 0 ? positive(x) : negative(x);
    }

    int positive(int x) { return x; }
    int negative(int x) { return -x; }
}
