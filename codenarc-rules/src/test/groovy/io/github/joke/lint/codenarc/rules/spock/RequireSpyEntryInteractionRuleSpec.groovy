package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class RequireSpyEntryInteractionRuleSpec extends Specification {

    private static final String MISSING =
            "Add '1 * service._' here, or '0 * _' fails on the call 'when:' made on the spy."

    private static final String MISORDERED =
            "Declare '1 * service._' after the specific interactions on the spy: " +
                    'declared first it absorbs the calls they verify.'

    Rule rule = new RequireSpyEntryInteractionRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'RequireSpyEntryInteraction'
        rule.priority == 2
    }

    def 'a spy with sibling interactions and no entry interaction is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    def 'a spy with the entry interaction is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    1 * service._
                    0 * _
                }
            }
        ''').empty
    }

    def 'the required tail also satisfies the strict mocking terminator'() {
        Rule terminator = new RequireStrictMockingTerminatorRule()
        String source = '''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    1 * repository.persist(order)
                    1 * service._
                    0 * _
                }
            }
        '''

        expect:
        violationsIn(source).empty
        terminator.applyTo(new SourceString(source)).empty
    }

    def 'two spies each require their own entry interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    AuditLog auditLog = Spy(AuditLog)
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    1 * auditLog.record(entry)
                    1 * service._
                    0 * _
                }
            }
        ''')*.message == ["Add '1 * auditLog._' here, or '0 * _' fails on the call 'when:' made on the spy."]
    }

    def 'each then block is judged separately'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    1 * service._
                    0 * _

                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    def 'an and block continues the then block it follows'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    and:
                    1 * service._
                    0 * _
                }
            }
        ''').empty
    }

    def 'a then block with no spy interaction is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * repository.persist(order)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a lone entry interaction does not trigger the requirement'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service._
                    0 * _
                }
            }
        ''').empty
    }

    def 'a feature method with no spy is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderRepository repository = Mock()
                    when:
                    service.placeOrder(order)
                    then:
                    1 * repository.persist(order)
                    0 * _
                }
            }
        ''').empty
    }

    def 'an entry interaction before the specific interactions is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service._
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''')*.message == [MISORDERED]
    }

    def 'an entry interaction between two specific interactions is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    1 * service._
                    1 * service.price(order)
                    0 * _
                }
            }
        ''')*.message == [MISORDERED]
    }

    def 'a misplaced entry interaction is reported once however many follow it'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service._
                    1 * service.validate(order)
                    1 * service.price(order)
                    0 * _
                }
            }
        ''')*.message == [MISORDERED]
    }

    def 'a misplaced entry interaction is reported in each then block'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service._
                    1 * service.validate(order)
                    0 * _

                    when:
                    service.placeOrder(order)
                    then:
                    1 * service._
                    1 * service.price(order)
                    0 * _
                }
            }
        ''')*.message == [MISORDERED, MISORDERED]
    }

    def 'an interaction on a spy outside a then block is not judged'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    1 * service.validate(order)

                    expect:
                    service.total == 49.99G
                }
            }
        ''').empty
    }

    def 'an entry interaction after a non-spy interaction is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    1 * repository.persist(order)
                    1 * service._
                    0 * _
                }
            }
        ''').empty
    }

    def 'a constrained wildcard call is a specific interaction rather than the entry one'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service._(order)
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    def 'a property interaction on a spy is a specific interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.total
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    def 'a bare variable interaction on a spy is a specific interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    def 'a stubbed return on a spy is a specific interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    service.validate(order) >> true
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    def 'a spy over a real instance is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(realService)
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    def 'an untyped spy declaration is recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def service = Spy(OrderService)
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    def 'a spy field is recognised in every feature method'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {

                OrderService service = Spy(constructorArgs: [repository])

                def 'a feature'() {
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }

                def 'another feature'() {
                    when:
                    service.cancelOrder(order)
                    then:
                    1 * service.release(order)
                    0 * _
                }
            }
        ''')*.message == [MISSING, MISSING]
    }

    def 'a field that is not a spy is not recognised'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {

                OrderRepository repository = Mock()
                OrderService service

                def 'a feature'() {
                    when:
                    service.placeOrder(order)
                    then:
                    1 * repository.persist(order)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a local spy does not carry into the next feature method'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {

                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    1 * service._
                    0 * _
                }

                def 'another feature'() {
                    when:
                    service.cancelOrder(order)
                    then:
                    1 * service.release(order)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a spy from an unrecognised source is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = spyOnService()
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a declaration whose initialiser is not a call is not a spy'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = realService
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a multiple assignment declares no spy'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def (service, repository) = Spy(constructorArgs: [])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''').empty
    }

    def 'a statement that is not an expression is not classified'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    assert service
                    0 * _
                }
            }
        ''').empty
    }

    def 'a bare call on a spy is not an interaction'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    service.warmUp()
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
                    OrderService service = Spy(constructorArgs: [repository])
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
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
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
                    OrderService service = Spy(constructorArgs: [repository])
                    when:
                    service.placeOrder(order)
                    then:
                    1 * service.validate(order)
                    0 * _
                }
            }
        ''')*.message == [MISSING]
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
