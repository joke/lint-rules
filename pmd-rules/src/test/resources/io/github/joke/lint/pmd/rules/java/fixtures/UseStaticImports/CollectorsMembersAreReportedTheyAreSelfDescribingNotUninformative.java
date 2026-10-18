import java.util.stream.Collectors;

public class Example {
    Object m(java.util.stream.Stream<String> s) {
        return s.collect(Collectors.toList());
    }
}
