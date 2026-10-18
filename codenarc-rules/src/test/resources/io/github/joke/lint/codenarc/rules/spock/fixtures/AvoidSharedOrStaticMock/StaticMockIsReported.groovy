import spock.lang.Specification

class ExampleSpec extends Specification {
    static Repository repository = Mock()

    def 'a feature'() {
        expect:
        repository
    }
}
