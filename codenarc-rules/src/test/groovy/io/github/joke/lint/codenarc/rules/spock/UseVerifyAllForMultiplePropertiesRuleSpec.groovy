package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class UseVerifyAllForMultiplePropertiesRuleSpec extends Specification {

    private static final String MESSAGE = "Assert the properties inside 'verifyAll', " +
            'so a value wrong in two fields reports both failures rather than the first.'

    Rule rule = new UseVerifyAllForMultiplePropertiesRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'UseVerifyAllForMultipleProperties'
        rule.priority == 2
    }

    def 'a chained conjunction is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ it.name == 'Ada' && it.region == 'eu' })
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'a three-way conjunction is reported once'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ it.name == 'Ada' && it.region == 'eu' && it.active })
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'two separate conditions are reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ it.name == 'Ada'; it.region == 'eu' })
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'a stubbed-return interaction is covered'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    repository.findById({ it.id == 'x' && it.active }) >> customer
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'a constraint closure outside a then block is covered'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    repository.findById({ it.id == 'x' && it.active }) >> customer

                    when:
                    service.register(customer)
                    then:
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'a closure delegating to verifyAll is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ Customer c ->
                        verifyAll(c) {
                            name == 'Ada'
                            region == 'eu'
                        }
                    })
                    0 * _
                }
            }
        ''').empty
    }

    def 'a closure delegating to with is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ Customer c ->
                        with(c) {
                            name == 'Ada'
                            region == 'eu'
                            active
                        }
                    })
                    0 * _
                }
            }
        ''').empty
    }

    def 'a closure delegating to verifyEach is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.saveAll({ List items ->
                        verifyEach(items) {
                            it.region == 'eu'
                        }
                    })
                    0 * _
                }
            }
        ''').empty
    }

    def 'a single-property closure is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * auditLog.record({ it.action == 'CHECKOUT' })
                    0 * _
                }
            }
        ''').empty
    }

    def 'a single condition using a relational operator is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ it.total > 0 })
                    0 * _
                }
            }
        ''').empty
    }

    def 'a disjunction is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ it.region == 'eu' || it.region == 'uk' })
                    0 * _
                }
            }
        ''').empty
    }

    def 'a statement that is not a condition does not count'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ def region = it.region; region == 'eu' })
                    0 * _
                }
            }
        ''').empty
    }

    def 'a statement that is not an expression does not count as a condition'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({
                        assert it.active
                        it.name == 'Ada'
                        it.region == 'eu'
                    })
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    def 'an argument that is not a closure is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save(expectedCustomer)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a statement that is not an expression carries no interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    assert repository.saved
                    0 * _
                }
            }
        ''').empty
    }

    def 'an always-true closure is reported by only the argument rule'() {
        Rule arguments = new RequireValidatedInteractionArgumentsRule()
        String source = '''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    service.register(customer)
                    then:
                    1 * repository.save({ true })
                    0 * _
                }
            }
        '''

        expect:
        arguments.applyTo(new SourceString(source)).size() == 1
        violationsIn(source).empty
    }

    def 'several assertions in an expect block are not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def receipt = service.checkout(order)

                    expect:
                    receipt.total == 49.99G
                    receipt.currency == 'EUR'
                    receipt.reference == 'r-1'
                }
            }
        ''').empty
    }

    def 'a chained conjunction in an expect block is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def receipt = service.checkout(order)

                    expect:
                    receipt.total == 49.99G && receipt.currency == 'EUR'
                }
            }
        ''').empty
    }

    def 'a fixture method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def setup() {
                    repository.findById({ it.id == 'x' && it.active }) >> customer
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
                    service.register(customer)
                    then:
                    1 * repository.save({ it.name == 'Ada' && it.region == 'eu' })
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
                    service.register(customer)
                    then:
                    1 * repository.save({ it.name == 'Ada' && it.region == 'eu' })
                    0 * _
                }
            }
        ''')*.message == [MESSAGE]
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
