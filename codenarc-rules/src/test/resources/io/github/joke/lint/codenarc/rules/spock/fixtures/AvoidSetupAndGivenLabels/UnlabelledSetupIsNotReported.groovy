import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        def gateway = 1
        when:
        def result = gateway
        then:
        result
    }
}
