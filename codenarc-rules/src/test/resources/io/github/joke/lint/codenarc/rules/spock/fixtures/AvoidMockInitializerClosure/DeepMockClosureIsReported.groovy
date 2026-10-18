import spock.lang.Specification

class ExampleSpec extends Specification {
    Repository repository = DeepMock { name = 'repo' }

    def 'a feature'() {
        expect:
        repository
    }
}
