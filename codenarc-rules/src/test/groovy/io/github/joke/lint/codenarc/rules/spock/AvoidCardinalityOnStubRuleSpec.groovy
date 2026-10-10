package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class AvoidCardinalityOnStubRuleSpec extends Specification {

    Rule rule = new AvoidCardinalityOnStubRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'AvoidCardinalityOnStub'
        rule.priority == 2
    }

    def 'a counted stub interaction is reported'() {
        expect:
        inThen('1 * repository.find(id)')*.message == [
                "Remove the cardinality from this interaction on stub 'repository': a stub is not verified, and Spock rejects a required interaction on one. Use '>> value', or make it a Mock.",
        ]
    }

    def 'a zero cardinality is reported'() {
        expect:
        inThen('0 * repository.find(id)').size() == 1
    }

    def 'a range cardinality is reported'() {
        expect:
        inThen('(1..3) * repository.find(id)').size() == 1
    }

    def 'a range reaching zero is reported'() {
        expect:
        inThen('(0..2) * repository.find(id)').size() == 1
    }

    def 'each counted interaction is reported'() {
        expect:
        inThen('''
            1 * repository.find(id)
            1 * repository.delete(id)
        ''').size() == 2
    }

    def 'a property access on a stub is reported'() {
        expect:
        inThen('1 * repository.name').size() == 1
    }

    def 'a counted and stubbed interaction is reported'() {
        expect:
        inThen('1 * repository.find(id) >> customer').size() == 1
    }

    def 'a counted interaction with a response sequence is reported'() {
        expect:
        inThen('1 * repository.find(id) >>> [first, second]').size() == 1
    }

    def 'a stubbed interaction with no cardinality is not reported'() {
        expect:
        inThen('repository.find(id) >> customer').empty
    }

    def 'a wildcard cardinality is not reported'() {
        expect:
        inThen('_ * repository.find(id)').empty
    }

    def 'a wildcard cardinality with a response is not reported'() {
        expect:
        inThen('_ * repository.find(id) >> customer').empty
    }

    def 'the strict mocking terminator is not reported'() {
        expect:
        inThen('''
            repository.find(id) >> customer
            0 * _
        ''').empty
    }

    def 'a wildcard receiver with a method is not reported'() {
        expect:
        inThen('1 * _.find(id)').empty
    }

    def 'a mock interaction with a cardinality is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    CustomerRepository repository = Mock()

                    when:
                    service.run()

                    then:
                    1 * repository.find(id)
                }
            }
        ''').empty
    }

    def 'a spy interaction with a cardinality is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])

                    when:
                    service.run()

                    then:
                    1 * service.validate(order) >> true
                }
            }
        ''').empty
    }

    def 'a stub field is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                CustomerRepository repository = Stub()

                def 'a feature'() {
                    when:
                    service.run()

                    then:
                    1 * repository.find(id)
                }
            }
        ''').size() == 1
    }

    def 'a stub declared with a named argument is recognised'() {
        expect:
        inThen('1 * repository.find(id)', "Stub(name: 'users')").size() == 1
    }

    def 'a stub declared with a type argument is recognised'() {
        expect:
        inThen('1 * repository.find(id)', 'Stub(CustomerRepository)').size() == 1
    }

    def 'a stub from a helper method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def collaborator = build()

                    when:
                    service.run()

                    then:
                    1 * collaborator.find(id)
                }
            }
        ''').empty
    }

    def 'a stub in one feature method is not a stub in the next'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'first'() {
                    CustomerRepository repository = Stub()

                    expect:
                    1 == 1
                }

                def 'second'() {
                    CustomerRepository repository = Mock()

                    when:
                    service.run()

                    then:
                    1 * repository.find(id)
                }
            }
        ''').empty
    }

    def 'a verified stub is not reported'() {
        expect:
        inThen('1 * repository.find(id)', 'Stub(verified: true)').empty
    }

    def 'a verified stub with a type argument is not reported'() {
        expect:
        inThen('1 * repository.find(id)', 'Stub(CustomerRepository, verified: true)').empty
    }

    def 'a stub with another named argument beside verified is not reported'() {
        expect:
        inThen('1 * repository.find(id)', "Stub(name: 'users', verified: true)").empty
    }

    def 'an interaction block is not read'() {
        expect:
        inThen('interaction { 1 * repository.find(id) }').empty
    }

    def 'a call that is not an interaction is not reported'() {
        expect:
        inThen('repository.find(id)').empty
    }

    def 'a statement that is not an expression is not reported'() {
        expect:
        inThen('assert true').empty
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            class PlainClass {
                def 'a feature'() {
                    CustomerRepository repository = Stub()

                    then:
                    1 * repository.find(id)
                }
            }
        ''').empty
    }

    def 'specificationClassNames widens the gate to a class that extends nothing'() {
        rule.specificationClassNames = 'PlainClass'

        expect:
        violationsIn('''
            class PlainClass {
                def 'a feature'() {
                    CustomerRepository repository = Stub()

                    then:
                    1 * repository.find(id)
                }
            }
        ''').size() == 1
    }

    private List inThen(String statements, String factory = 'Stub()') {
        violationsIn("""
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    CustomerRepository repository = $factory

                    when:
                    service.run()

                    then:
                    $statements
                }
            }
        """)
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
