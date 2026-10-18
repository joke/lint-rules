import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        def repository = Mock(Repository)
        expect:
        repository
    }
}
