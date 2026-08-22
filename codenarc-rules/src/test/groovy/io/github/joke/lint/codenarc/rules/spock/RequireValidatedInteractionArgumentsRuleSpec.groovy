package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class RequireValidatedInteractionArgumentsRuleSpec extends Specification {

    Rule rule = new RequireValidatedInteractionArgumentsRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'RequireValidatedInteractionArguments'
        rule.priority == 2
    }

    def 'a bare wildcard argument is reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save(_)'))*.message ==
                ['Pass the value the interaction expects: an argument that matches anything asserts only that a call happened.']
    }

    def 'a typed wildcard is reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save(_ as Customer)')).size() == 1
    }

    def 'a spread wildcard is reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save(*_)')).size() == 1
    }

    def 'an always-true closure is reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ true })')).size() == 1
    }

    def 'an always-true closure with an underscore parameter is reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ _ -> true })')).size() == 1
    }

    def 'an always-true closure with an it parameter is reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ it -> true })')).size() == 1
    }

    def 'a truthy constant other than true is reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ 1 })')).size() == 1
    }

    def 'a non-empty string constant is reported'() {
        expect:
        violationsIn(thenBlockOf("1 * repository.save({ 'anything' })")).size() == 1
    }

    def 'each unconstrained argument is reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.saveBoth(_, _)')).size() == 2
    }

    def 'an exact value is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save(expectedCustomer)')).empty
    }

    def 'a discriminating constraint closure is not reported'() {
        expect:
        violationsIn(thenBlockOf("1 * repository.save({ it.id == 'cust-1' })")).empty
    }

    def 'a verifyAll constraint closure is not reported'() {
        expect:
        violationsIn(thenBlockOf("1 * repository.save({ verifyAll(it) { id == 'cust-1' } })")).empty
    }

    def 'a closure returning its own parameter is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ it })')).empty
    }

    def 'a falsy constant closure is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ 0 })')).empty
    }

    def 'a null constant closure is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ null })')).empty
    }

    def 'a closure of several statements is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ log(it); true })')).empty
    }

    def 'an empty closure is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ })')).empty
    }

    def 'a closure returning a constant explicitly is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.save({ return true })')).empty
    }

    def 'a stubbed return without cardinality is reported'() {
        expect:
        violationsIn(thenBlockOf('repository.findById(_) >> customer')).size() == 1
    }

    def 'a stubbed sequence of returns is reported'() {
        expect:
        violationsIn(thenBlockOf('repository.findById(_) >>> [customer]')).size() == 1
    }

    def 'a combined cardinality and stub is reported once'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.findById(_) >> customer')).size() == 1
    }

    def 'an interaction on a stub is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    CustomerRepository repository = Stub()

                    expect:
                    repository.findById(_) >> customer
                }
            }
        ''').size() == 1
    }

    def 'an interaction on a spy is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    CustomerRepository repository = Spy()

                    expect:
                    1 * repository.save(_)
                }
            }
        ''').size() == 1
    }

    def 'an interaction inside a mock initialiser closure is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                CustomerRepository repository = Mock() {
                    findById(_) >> row
                }

                def 'a feature'() {
                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'an interaction inside a mock initialiser on a local declaration is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    CustomerRepository repository = Mock() {
                        findById(_) >> row
                    }

                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'the mock initialiser is reported by its own rule as well, because each rule stands alone'() {
        expect:
        violationsIn(mockInitialiser).size() == 1
        new AvoidMockInitializerClosureRule().applyTo(new SourceString(mockInitialiser)).size() == 1

        where:
        mockInitialiser = '''
            class ExampleSpec extends Specification {
                CustomerRepository repository = Mock() {
                    findById(_) >> row
                }

                def 'a feature'() {
                    expect:
                    receipt
                }
            }
        '''
    }

    def 'the strict mocking terminator is not reported'() {
        expect:
        violationsIn(thenBlockOf('0 * _')).empty
    }

    def 'a spy entry interaction is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * service._')).empty
    }

    def 'a wildcard method name is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository._(expectedCustomer)')).empty
    }

    def 'an interaction taking no argument is not reported'() {
        expect:
        violationsIn(thenBlockOf('1 * repository.deleteAll()')).empty
    }

    def 'an interaction in the setup region is still checked'() {
        expect:
        violationsIn(setupRegion).size() == 1
        new InteractionsBelongInThenBlockRule().applyTo(new SourceString(setupRegion)).size() == 1

        where:
        setupRegion = '''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    1 * repository.save(_)

                    expect:
                    receipt
                }
            }
        '''
    }

    def 'an ordinary call passing the wildcard is not an interaction'() {
        expect:
        violationsIn(thenBlockOf('repository.save(_)')).empty
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            class PlainClass {
                def 'a method'() {
                    expect:
                    1 * repository.save(_)
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
                    expect:
                    1 * repository.save(_)
                }
            }
        ''').size() == 1
    }

    private static String thenBlockOf(String interaction) {
        """
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    ${interaction}
                }
            }
        """
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
