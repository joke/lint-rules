import spock.lang.Specification

class ExampleSpec extends Specification {
    def 'a feature'() {
        SpyStatic(PricingRules)
        when:
        service.run()
        then:
        true
    }
}
