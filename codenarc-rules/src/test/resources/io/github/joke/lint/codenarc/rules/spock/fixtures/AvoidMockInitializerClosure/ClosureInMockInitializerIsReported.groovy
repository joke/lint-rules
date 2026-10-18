import spock.lang.Specification

class ExampleSpec extends Specification {
    Repository repository = Mock { name = 'repo' }

    def 'a feature'() {
        expect:
        repository
    }
}
