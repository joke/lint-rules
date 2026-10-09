package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class RequireSpyInteractionResponseRuleSpec extends Specification {

    Rule rule = new RequireSpyInteractionResponseRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'RequireSpyInteractionResponse'
        rule.priority == 2
    }

    def 'a spy interaction with no response is reported'() {
        expect:
        inThen('1 * service.validate(order)')*.message == [
                "State the response of this interaction on spy 'service': without '>>' it calls the real method. Use '>> value', or '>> { callRealMethod() }' if that is meant.",
        ]
    }

    def 'a spy interaction with a value response is not reported'() {
        expect:
        inThen('1 * service.validate(order) >> true').empty
    }

    def 'a spy interaction with a closure response is not reported'() {
        expect:
        inThen('1 * service.validate(order) >> { callRealMethod() }').empty
    }

    def 'a spy interaction with a response sequence is not reported'() {
        expect:
        inThen('1 * service.validate(order) >>> [true, false]').empty
    }

    def 'a wildcard cardinality is reported'() {
        expect:
        inThen('_ * service.validate(order)').size() == 1
    }

    def 'a range cardinality is reported'() {
        expect:
        inThen('(1..3) * service.validate(order)').size() == 1
    }

    def 'each unstubbed interaction is reported'() {
        expect:
        inThen('''
            1 * service.validate(order)
            1 * service.audit(order)
        ''').size() == 2
    }

    def 'a property access on a spy is reported'() {
        expect:
        inThen('1 * service.name').size() == 1
    }

    def 'a bare spy variable is reported'() {
        expect:
        inThen('1 * service').size() == 1
    }

    def 'the entry interaction is not reported'() {
        expect:
        inThen('1 * service._').empty
    }

    def 'the entry interaction at another cardinality is not reported'() {
        expect:
        inThen('2 * service._').empty
    }

    def 'a wildcard method with an argument is reported'() {
        expect:
        inThen('1 * service._(order)').size() == 1
    }

    def 'a pattern-matched method name is reported'() {
        expect:
        inThen('1 * service./validate.*/(order)').size() == 1
    }

    def 'a never-called spy method is not reported'() {
        expect:
        inThen('0 * service.audit(order)').empty
    }

    def 'the required tail satisfies this rule and the entry rule'() {
        def source = feature('''
            1 * service.validate(order) >> true
            1 * service._
            0 * _
        ''')

        expect:
        rule.applyTo(new SourceString(source)).empty
        new RequireSpyEntryInteractionRule().applyTo(new SourceString(source)).empty
    }

    def 'a mock interaction with no response is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    CustomerRepository repository = Mock()

                    when:
                    service.run()

                    then:
                    1 * repository.persist(order)
                }
            }
        ''').empty
    }

    def 'a spy field is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                OrderService service = Spy(constructorArgs: [repo])

                def 'a feature'() {
                    when:
                    service.run()

                    then:
                    1 * service.validate(order)
                }
            }
        ''').size() == 1
    }

    def 'a spy declared in setup is recognised in a later block'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(realService)

                    when:
                    service.run()

                    then:
                    1 * service.validate(order)
                }
            }
        ''').size() == 1
    }

    def 'a subject from a helper method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def subject = build()

                    when:
                    subject.run()

                    then:
                    1 * subject.validate(order)
                }
            }
        ''').empty
    }

    def 'a spy in one feature method is not a spy in the next'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'first'() {
                    OrderService service = Spy(realService)

                    expect:
                    1 == 1
                }

                def 'second'() {
                    def service = build()

                    when:
                    service.run()

                    then:
                    1 * service.validate(order)
                }
            }
        ''').empty
    }

    def 'an interaction block is not read'() {
        expect:
        inThen('interaction { 1 * service.validate(order) }').empty
    }

    def 'a call that is not an interaction is not reported'() {
        expect:
        inThen('service.validate(order)').empty
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
                    def service = Spy(Foo)

                    then:
                    1 * service.validate(order)
                }
            }
        ''').empty
    }

    private List inThen(String statements) {
        violationsIn(feature(statements))
    }

    private String feature(String statements) {
        """
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])

                    when:
                    service.placeOrder(order)

                    then:
                    $statements
                }
            }
        """
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
