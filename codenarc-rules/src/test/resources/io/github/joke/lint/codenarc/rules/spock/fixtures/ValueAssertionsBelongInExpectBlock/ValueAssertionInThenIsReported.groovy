import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        when:
        def receipt = service.run()
        then:
        receipt.total == 49.99G
    }
}
