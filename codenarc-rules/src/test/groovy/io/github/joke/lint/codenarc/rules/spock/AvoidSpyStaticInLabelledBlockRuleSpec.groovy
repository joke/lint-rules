package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class AvoidSpyStaticInLabelledBlockRuleSpec extends Specification {

    private static final String MESSAGE = "Call 'SpyStatic' with the unlabelled setup statements: " +
            'it arranges the whole feature method rather than verifying one call.'

    Rule rule = new AvoidSpyStaticInLabelledBlockRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'AvoidSpyStaticInLabelledBlock'
        rule.priority == 3
    }

    def 'SpyStatic in a then block is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    SpyStatic(PricingRules)
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'SpyStatic in a when block is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    SpyStatic(PricingRules)
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'SpyStatic in a given block is reported by this rule and by the label rule'() {
        Rule labels = new AvoidSetupAndGivenLabelsRule()
        String source = '''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    given:
                    SpyStatic(PricingRules)

                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    0 * _
                }
            }
        '''

        expect:
        violationsIn(source)*.message == [MESSAGE]
        labels.applyTo(new SourceString(source)).size() == 1
    }

    def 'SpyStatic under an explicit setup label is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    setup:
                    SpyStatic(PricingRules)

                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'SpyStatic in the unlabelled setup region is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    SpyStatic(PricingRules)
                    def service = new InvoiceService()

                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    1 * PricingRules.taxRate('eu') >> 0.20G
                    0 * _
                }
            }
        ''').empty
    }

    def 'a feature method opening with a label has an empty setup region'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    0 * _
                }
            }
        ''').empty
    }

    def 'an unrelated call under a label is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.totalWithTax(100G, 'eu')
                    then:
                    0 * _
                }
            }
        ''').empty
    }

    def 'a name that merely ends in SpyStatic is not matched'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    mySpyStatic(PricingRules)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a statement that is not an expression is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    assert total
                    0 * _
                }
            }
        ''').empty
    }

    def 'a fixture method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def setup() {
                    SpyStatic(PricingRules)
                }
            }
        ''').empty
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            class PlainClass {
                def 'a method'() {
                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    SpyStatic(PricingRules)
                    0 * _
                }
            }
        ''').empty
    }

    def 'specificationClassNames widens the gate to a class that extends nothing'() {
        rule.specificationClassNames = 'PlainClass'

        expect:
        violationsIn('''
            class PlainClass {
                def 'a method'() {
                    when:
                    def total = service.totalWithTax(100G, 'eu')
                    then:
                    SpyStatic(PricingRules)
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
