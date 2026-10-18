import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        Repository repository = DeepMock(Repository)
        expect:
        repository
    }
}
