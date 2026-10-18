import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        when:
        def receipt = service.run()
        then:
        1 * repository.save({ it.total == 49.99G })
    }
}
