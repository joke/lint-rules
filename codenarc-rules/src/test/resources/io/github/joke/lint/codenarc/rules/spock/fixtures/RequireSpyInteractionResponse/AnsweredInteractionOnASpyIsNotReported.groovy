import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        given:
        Service service = Spy()
        when:
        service.run()
        then:
        1 * service.run() >> 'done'
    }
}
