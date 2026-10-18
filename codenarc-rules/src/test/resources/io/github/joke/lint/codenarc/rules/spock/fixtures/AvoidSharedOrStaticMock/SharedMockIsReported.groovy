import spock.lang.Shared
import spock.lang.Specification

class ExampleSpec extends Specification {
    @Shared
    Repository repository = Mock()

    def 'a feature'() {
        expect:
        repository
    }
}
