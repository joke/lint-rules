import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        when:
        service.run()
        then:
        1 * repository.save(_)
        0 * _
    }
}
