import spock.lang.Specification

class ExampleSpec extends Specification {
    Repository repository = Mock()

    def 'a feature'() {
        expect:
        repository
    }
}
