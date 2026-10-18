import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        when:
        service.run()
        then:
        thrown(IllegalArgumentException)
    }
}
