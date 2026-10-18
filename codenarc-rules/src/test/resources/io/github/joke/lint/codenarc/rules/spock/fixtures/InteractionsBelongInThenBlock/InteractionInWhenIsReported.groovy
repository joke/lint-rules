import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        when:
        service.run()
        1 * repository.save(_)
        then:
        true
    }
}
