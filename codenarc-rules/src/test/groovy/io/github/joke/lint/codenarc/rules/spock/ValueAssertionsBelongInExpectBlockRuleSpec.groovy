package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class ValueAssertionsBelongInExpectBlockRuleSpec extends Specification {

    Rule rule = new ValueAssertionsBelongInExpectBlockRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'ValueAssertionsBelongInExpectBlock'
        rule.priority == 2
    }

    def 'an equality assertion in a then block is reported'() {
        expect:
        violationsIn(thenBlockOf("receipt.transactionId == 'txn-123'"))*.message ==
                ["'then:' states who was called; move the value assertion to 'expect:'."]
    }

    def 'a relational assertion in a then block is reported'() {
        expect:
        violationsIn(thenBlockOf('total > 0')).size() == 1
    }

    def 'an assertion in an and block after then is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    1 * repository.save(customer)
                    and:
                    receipt.total == 49.99G
                }
            }
        ''').size() == 1
    }

    def 'several assertions are reported separately'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    receipt.total == 49.99G
                    receipt.currency == 'EUR'
                }
            }
        ''').size() == 2
    }

    def 'the same assertion in an expect block is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    expect:
                    receipt.transactionId == 'txn-123'
                }
            }
        ''').empty
    }

    def 'an interaction is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save(customer) >> customer')).empty
    }

    def 'the strict mocking terminator is not reported'() {
        expect:
        violationsIn(thenBlockOf('0 * _')).empty
    }

    def 'a thrown call is not reported'() {
        expect:
        violationsIn(thenBlockOf('thrown(IllegalArgumentException)')).empty
    }

    def 'a notThrown call is not reported'() {
        expect:
        violationsIn(thenBlockOf('notThrown(IllegalArgumentException)')).empty
    }

    def 'a captured exception declaration is not reported'() {
        expect:
        violationsIn(thenBlockOf('def error = thrown(IllegalArgumentException)')).empty
    }

    def 'the exception message assertion is reported'() {
        expect:
        violationsIn(thenBlockOf("error.message == 'order must not be null'")).size() == 1
    }

    def 'a helper call in a then block is not reported'() {
        expect:
        violationsIn(thenBlockOf('service.warmUp()')).empty
    }

    def 'a bare truthiness check is not reported'() {
        expect:
        violationsIn(thenBlockOf('receipt.valid')).empty
    }

    def 'a boolean-named method call is reported'() {
        expect:
        violationsIn(thenBlockOf('receipt.isValid()')).size() == 1
    }

    def 'a statement that is not an expression is not reported'() {
        expect:
        violationsIn(thenBlockOf('assert receipt.valid')).empty
    }

    def 'a fixture method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def setup() {
                    receipt.total == 49.99G
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
                    def receipt = service.checkout(order)
                    then:
                    receipt.total == 49.99G
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
                    def receipt = service.checkout(order)
                    then:
                    receipt.total == 49.99G
                }
            }
        ''').size() == 1
    }

    private static String thenBlockOf(String statement) {
        """
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    ${statement}
                }
            }
        """
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
