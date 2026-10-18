public class Example {
    void m() throws java.io.IOException {
        try (java.io.InputStream in = open()) { }
    }

    java.io.InputStream open() {
        return null;
    }
}
