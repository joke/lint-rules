package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class DeclareMockWithExplicitTypeRuleSpec extends Specification {

    Rule rule = new DeclareMockWithExplicitTypeRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'DeclareMockWithExplicitType'
        rule.priority == 2
    }

    def 'an untyped Mock declaration is reported'() {
        expect:
        inFeature('def repository = Mock(CustomerRepository)')*.message ==
                ["Declare the collaborator's type on the left: 'Type name = Mock()' rather than 'def name = Mock(Type)'."]
    }

    def 'an untyped Stub declaration is reported'() {
        expect:
        inFeature('def repository = Stub(CustomerRepository)').size() == 1
    }

    def 'an untyped Spy declaration is reported'() {
        expect:
        inFeature('def service = Spy(OrderService, constructorArgs: [repository])').size() == 1
    }

    def 'a typed declaration is not reported'() {
        expect:
        inFeature('CustomerRepository repository = Mock()').empty
    }

    def 'a typed Spy with constructor arguments is not reported'() {
        expect:
        inFeature('OrderService service = Spy(constructorArgs: [repository])').empty
    }

    def 'a mock passed inline as a constructor argument is not reported'() {
        expect:
        inFeature('def service = new CheckoutService(Mock(PaymentGateway))').empty
    }

    def 'a mock passed inline as a method argument is not reported'() {
        expect:
        inFeature('service.register(Mock(Listener))').empty
    }

    def 'a dynamically typed declaration of something else is not reported'() {
        expect:
        inFeature('def service = new CheckoutService(gateway)').empty
    }

    def 'a dynamically typed declaration from an unrelated call is not reported'() {
        expect:
        inFeature('def service = build(CheckoutService)').empty
    }

    def 'a multiple assignment declaration is not reported'() {
        expect:
        inFeature('def (repository, gateway) = [Mock(Repo), Mock(Gateway)]').empty
    }

    def 'an untyped mock field is reported'() {
        expect:
        inClassBody('def repository = Mock(CustomerRepository)').size() == 1
    }

    def 'a typed mock field is not reported'() {
        expect:
        inClassBody('CustomerRepository repository = Mock()').empty
    }

    def 'a private untyped mock field is reported'() {
        expect:
        inClassBody('private def repository = Mock(CustomerRepository)').size() == 1
    }

    def 'an untyped field initialised from something else is not reported'() {
        expect:
        inClassBody('def counter = 0').empty
    }

    def 'a field declared outside a feature method is still reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def repository = Mock(CustomerRepository)
            }
        ''').size() == 1
    }

    def 'a mock declared inside a closure initialiser is reported'() {
        expect:
        inFeature('def factory = { def repository = Mock(Repo) }').size() == 1
    }

    def 'a mock declared inside a private field closure initialiser is reported'() {
        expect:
        inClassBody('private def factory = { def repository = Mock(Repo) }').size() == 1
    }

    def 'a mock inside a property closure initialiser is reported once, not once per traversal'() {
        expect:
        inClassBody('def factory = { def repository = Mock(Repo) }').size() == 1
    }

    def 'a typed Stub repeating its type is reported'() {
        expect:
        inFeature('TypeMirror mirror = Stub(TypeMirror)')*.message ==
                ["Drop the type argument: the declared type already says it, so write 'Type name = Mock()'."]
    }

    def 'a typed Mock repeating its type is reported'() {
        expect:
        inFeature('CustomerRepository repository = Mock(CustomerRepository)').size() == 1
    }

    def 'a typed Spy repeating its type with constructor arguments is reported'() {
        expect:
        inFeature('OrderService service = Spy(OrderService, constructorArgs: [repository])').size() == 1
    }

    def 'a repeated type on a generic declaration is reported'() {
        expect:
        inFeature('List<String> items = Mock(List)').size() == 1
    }

    def 'a repeated type written with a class suffix is reported'() {
        expect:
        inFeature('CustomerRepository repository = Mock(CustomerRepository.class)').size() == 1
    }

    def 'a repeated type written qualified is reported'() {
        expect:
        inFeature('List<String> items = Mock(java.util.List)').size() == 1
    }

    def 'a qualified declared type repeated unqualified is reported'() {
        expect:
        inFeature('java.util.List<String> items = Mock(List)').size() == 1
    }

    def 'a nested type repeated is reported'() {
        expect:
        inFeature('Outer.Inner inner = Mock(Outer.Inner)').size() == 1
    }

    def 'a different nested type is not reported'() {
        expect:
        inFeature('Outer.Inner inner = Mock(Other.Inner)').empty
    }

    def 'two differently qualified types of one simple name are not reported'() {
        expect:
        inFeature('java.util.List items = Mock(java.awt.List)').empty
    }

    def 'a type of a longer name ending in the declared name is not reported'() {
        expect:
        inFeature('Repository repository = Mock(CustomerRepository)').empty
    }

    def 'a repeated type on a typed field is reported'() {
        expect:
        inClassBody('CustomerRepository repository = Mock(CustomerRepository)').size() == 1
    }

    def 'a repeated type with an initialiser closure is reported'() {
        expect:
        inFeature('CustomerRepository repository = Mock(CustomerRepository) { findById(_) >> row }').size() == 1
    }

    def 'a different type in the factory call is not reported'() {
        expect:
        inFeature('Collection<String> items = Mock(List)').empty
    }

    def 'a spy over a real instance is not reported'() {
        expect:
        inFeature('OrderService service = Spy(realService)').empty
    }

    def 'a typed declaration with no type argument is not reported'() {
        expect:
        inFeature('TypeMirror mirror = Stub()').empty
    }

    def 'a typed declaration with only named arguments is not reported'() {
        expect:
        inFeature("TypeMirror mirror = Stub(name: 'mirror')").empty
    }

    def 'a typed declaration initialised from something else is not reported'() {
        expect:
        inFeature('TypeMirror mirror = build(TypeMirror)').empty
    }

    def 'a typed declaration with a non-type argument is not reported'() {
        expect:
        inFeature("TypeMirror mirror = Stub('TypeMirror')").empty
    }

    def 'an untyped declaration repeating a type is reported once'() {
        expect:
        inFeature('def mirror = Stub(TypeMirror)').size() == 1
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            class PlainClass {
                def x = Mock(Foo)
                def method() {
                    def y = Mock(Foo)
                }
            }
        ''').empty
    }

    def 'specificationClassNames widens the gate to a class that extends nothing'() {
        rule.specificationClassNames = 'PlainClass'

        expect:
        violationsIn('''
            class PlainClass {
                def x = Mock(Foo)
            }
        ''').size() == 1
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
