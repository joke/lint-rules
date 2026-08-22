package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class InteractionsBelongInThenBlockRuleSpec extends Specification {

    Rule rule = new InteractionsBelongInThenBlockRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'InteractionsBelongInThenBlock'
        rule.priority == 2
    }

    def 'an interaction in the setup region is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    1 * repository.save(customer)

                    when:
                    def receipt = service.checkout(order)
                    then:
                    receipt
                }
            }
        ''')*.message == ["Declare the interaction in 'then:', where it verifies the 'when:' that caused it."]
    }

    def 'a stubbed return in the setup region is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    repository.findById('cust-1') >> customer

                    when:
                    def receipt = service.checkout(order)
                    then:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'a stubbed sequence of returns in the setup region is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    repository.findAll() >>> [first, second]

                    when:
                    def receipt = service.checkout(order)
                    then:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'an interaction in a when block is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    1 * repository.save(customer)
                    then:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'an interaction in an expect block is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    expect:
                    1 * repository.save(customer)
                }
            }
        ''').size() == 1
    }

    def 'an interaction in a then block is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    then:
                    1 * repository.save(customer) >> customer
                }
            }
        ''').empty
    }

    def 'a value assertion outside a then block is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    expect:
                    receipt.total == 49.99G
                }
            }
        ''').empty
    }

    def 'an interaction in an and block after then is not reported'() {
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
                }
            }
        ''').empty
    }

    def 'an interaction in an and block after when is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def receipt = service.checkout(order)
                    and:
                    1 * repository.save(customer)
                    then:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'an empty when block does not swallow the then block that follows it'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    then:
                    1 * repository.save(customer)
                }
            }
        ''').empty
    }

    def 'a range cardinality is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    (1..3) * repository.save(customer)

                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'a wildcard cardinality is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    _ * repository.save(customer)

                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'the combined form is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    1 * repository.findById('x') >> customer

                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'a property target is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    1 * service._

                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'a bare variable target is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    0 * _

                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'an interaction block is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    interaction { 1 * repository.save(customer) }

                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    def 'an interaction call without a closure is not an interaction block'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    interaction(handler)

                    expect:
                    receipt
                }
            }
        ''').empty
    }

    def 'another call taking a closure is not an interaction block'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    report { true }

                    expect:
                    receipt
                }
            }
        ''').empty
    }

    def 'an ordinary method call is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    service.warmUp()

                    expect:
                    receipt
                }
            }
        ''').empty
    }

    def 'an arithmetic multiplication is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def total = 2 * price

                    expect:
                    total
                }
            }
        ''').empty
    }

    def 'a multiplication whose right side is arithmetic is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    2 * (price + tax)

                    expect:
                    receipt
                }
            }
        ''').empty
    }

    def 'a multiplication whose count is not a literal is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    1 * price * quantity

                    expect:
                    receipt
                }
            }
        ''').empty
    }

    def 'a decimal multiplier is not a cardinality'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    1.5 * price

                    expect:
                    receipt
                }
            }
        ''').empty
    }

    def 'a comparison against a literal is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    expect:
                    0 < receipt.total
                }
            }
        ''').empty
    }

    def 'a stubbed return whose counted target is arithmetic is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    1 * (price + tax) >> total

                    expect:
                    receipt
                }
            }
        ''').empty
    }

    def 'a statement that is not an expression is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    expect:
                    assert receipt
                }
            }
        ''').empty
    }

    def 'a stubbed return whose left side is arithmetic is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    (price + tax) >> total

                    expect:
                    receipt
                }
            }
        ''').empty
    }

    def 'a fixture method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def setup() {
                    1 * repository.save(customer)
                }
            }
        ''').empty
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            class PlainClass {
                def 'a method'() {
                    1 * repository.save(customer)

                    expect:
                    receipt
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
                    1 * repository.save(customer)

                    expect:
                    receipt
                }
            }
        ''').size() == 1
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
