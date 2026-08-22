package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class RequireStrictMockingTerminatorRuleSpec extends Specification {

    Rule rule = new RequireStrictMockingTerminatorRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'RequireStrictMockingTerminator'
        rule.priority == 2
    }

    def 'a then block ending with the terminator is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    1 * repository.save(customer)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a then block with no terminator is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    1 * repository.save(customer)
                }
            }
        ''')*.message == ["End 'then:' with '0 * _', so an interaction nobody declared fails the specification."]
    }

    def 'a terminator that is not last is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    0 * _
                    1 * repository.save(customer)
                }
            }
        ''').size() == 1
    }

    def 'a non-zero cardinality on the wildcard is not a terminator'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    1 * _
                }
            }
        ''').size() == 1
    }

    def 'a zero cardinality on a named collaborator is not a terminator'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    0 * repository
                }
            }
        ''').size() == 1
    }

    def 'a method call is not a terminator'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    service.warmUp()
                }
            }
        ''').size() == 1
    }

    def 'a statement that is not an expression is not a terminator'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    assert receipt
                }
            }
        ''').size() == 1
    }

    def 'each then block is judged separately'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def first = service.checkout(order)
                    then:
                    1 * repository.save(customer)
                    0 * _

                    when:
                    def second = service.checkout(order)
                    then:
                    1 * repository.save(customer)
                }
            }
        ''').size() == 1
    }

    def 'a then block with no mocks in scope is still reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    rule.name = 'Renamed'
                    then:
                    rule.name == 'Renamed'
                }
            }
        ''').size() == 1
    }

    def 'a then block containing only the terminator is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    rule.name = 'Renamed'
                    then:
                    0 * _

                    expect:
                    rule.name == 'Renamed'
                }
            }
        ''').empty
    }

    def 'a terminator at the end of a trailing and block is accepted'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    1 * repository.save(customer)
                    and:
                    1 * eventBus.publish(event)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a terminator before a trailing and block is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    1 * repository.save(customer)
                    0 * _
                    and:
                    1 * eventBus.publish(event)
                }
            }
        ''').size() == 1
    }

    def 'a then and and run with no terminator anywhere is reported once'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    1 * repository.save(customer)
                    and:
                    1 * eventBus.publish(event)
                    and:
                    1 * auditLog.record(entry)
                }
            }
        ''').size() == 1
    }

    def 'a feature method with only an expect block is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def receipt = service.checkout(order)

                    expect:
                    receipt.total == 49.99G
                }
            }
        ''').empty
    }

    def 'the other blocks are not required to carry a terminator'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    0 * _

                    expect:
                    receipt.total == 49.99G

                    cleanup:
                    service.close()

                    where:
                    order << [firstOrder, secondOrder]
                }
            }
        ''').empty
    }

    def 'a fixture method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def setup() {
                    service.warmUp()
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
                    1 * repository.save(customer)
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
                    1 * repository.save(customer)
                }
            }
        ''').size() == 1
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
