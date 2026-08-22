package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.ruleset.CompositeRuleSet
import org.codenarc.ruleset.ListRuleSet
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class AvoidMockInitializerClosureRuleSpec extends Specification {

    Rule rule = new AvoidMockInitializerClosureRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'AvoidMockInitializerClosure'
        rule.priority == 2
    }

    def 'a Mock initialiser closure is reported'() {
        expect:
        inFeature('Repo repository = Mock() { findById(_) >> row }')*.message ==
                ["Declare the interaction in 'then:' with its cardinality and its real argument, not in the mock's initialiser."]
    }

    def 'a Stub initialiser closure is reported'() {
        expect:
        inFeature('Repo repository = Stub() { findById(_) >> row }').size() == 1
    }

    def 'a Spy initialiser closure is reported'() {
        expect:
        inFeature('Service service = Spy() { helper() >> 1 }').size() == 1
    }

    def 'a typed initialiser closure is reported'() {
        expect:
        inFeature('def repository = Mock(Repo) { findById(_) >> row }').size() == 1
    }

    def 'a mock initialiser field closure is reported once, not once per traversal'() {
        expect:
        inClassBody('Repo repository = Mock() { findById(_) >> row }').size() == 1
    }

    def 'an untyped mock initialiser field closure is reported once'() {
        expect:
        inClassBody('def repository = Mock() { findById(_) >> row }').size() == 1
    }

    def 'a bare mock call is not reported'() {
        expect:
        inFeature('Repo repository = Mock()').empty
    }

    def 'a typed mock call is not reported'() {
        expect:
        inFeature('def repository = Mock(Repo)').empty
    }

    def 'a spy with constructor arguments is not reported'() {
        expect:
        inFeature('Service service = Spy(constructorArgs: [repository])').empty
    }

    def 'a closure argument to an unrelated call is not reported'() {
        expect:
        inFeature('list.each { it }').empty
    }

    def 'an inline stubbed mock passed as an argument is reported'() {
        expect:
        inFeature('service.register(Mock(Listener) { bar() >> 1 })').size() == 1
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            class PlainClass {
                def method() {
                    def x = Mock(Foo) { }
                }
            }
        ''').empty
    }

    def 'specificationClassNames widens the gate to a class that extends nothing'() {
        rule.specificationClassNames = 'PlainClass'

        expect:
        violationsIn('''
            class PlainClass {
                def method() {
                    def x = Mock(Foo) { bar() >> 1 }
                }
            }
        ''').size() == 1
    }

    def 'a declaration that violates both rules is reported by both'() {
        def ruleSet = new CompositeRuleSet()
        ruleSet.addRuleSet(new ListRuleSet([rule, new DeclareMockWithExplicitTypeRule()]))
        def source = new SourceString("""
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    expect:
                    def repository = Mock(Repo) { findById(_) >> row }
                }
            }
        """)

        expect:
        ruleSet.rules.collectMany { it.applyTo(source) }*.rule*.name.toSorted() ==
                ['AvoidMockInitializerClosure', 'DeclareMockWithExplicitType']
    }

    private List inFeature(String statement) {
        violationsIn("""
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    expect:
                    $statement
                }
            }
        """)
    }

    private List inClassBody(String declaration) {
        violationsIn("""
            class ExampleSpec extends Specification {
                $declaration

                def 'a feature'() {
                    expect:
                    1 == 1
                }
            }
        """)
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
