package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class AvoidSharedOrStaticMockRuleSpec extends Specification {

    Rule rule = new AvoidSharedOrStaticMockRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'AvoidSharedOrStaticMock'
        rule.priority == 2
    }

    def 'a shared Mock field is reported'() {
        expect:
        inClassBody('@Shared CustomerRepository repository = Mock()')*.message ==
                ['Create the double inside the feature method: a shared or static one lives outside the scope Spock verifies its interactions in.']
    }

    def 'a shared Stub field is reported'() {
        expect:
        inClassBody('@Shared CustomerRepository repository = Stub()').size() == 1
    }

    def 'a shared Spy field is reported'() {
        expect:
        inClassBody('@Shared OrderService service = Spy(constructorArgs: [repository])').size() == 1
    }

    def 'a fully qualified Shared annotation is reported'() {
        expect:
        inClassBody('@spock.lang.Shared CustomerRepository repository = Mock()').size() == 1
    }

    def 'a static Mock field is reported'() {
        expect:
        inClassBody('static CustomerRepository repository = Mock()').size() == 1
    }

    def 'a static final field is reported'() {
        expect:
        inClassBody('static final CustomerRepository repository = Stub()').size() == 1
    }

    def 'a private shared field is reported once'() {
        expect:
        inClassBody('@Shared private CustomerRepository repository = Mock()').size() == 1
    }

    def 'an instance field is not reported'() {
        expect:
        inClassBody('CustomerRepository repository = Mock()').empty
    }

    def 'a field with an unrelated annotation is not reported'() {
        expect:
        inClassBody('@Deprecated CustomerRepository repository = Mock()').empty
    }

    def 'a shared field of something else is not reported'() {
        expect:
        inClassBody('@Shared Clock clock = Clock.systemUTC()').empty
    }

    def 'a shared field with no initialiser is not reported'() {
        expect:
        inClassBody('@Shared CustomerRepository repository').empty
    }

    def 'a shared field assigned a double in setupSpec is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                @Shared CustomerRepository repository

                def setupSpec() {
                    repository = Mock()
                }
            }
        ''').empty
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            class PlainClass {
                static Foo foo = Mock()
            }
        ''').empty
    }

    private List inClassBody(String declaration) {
        violationsIn("""
            class ExampleSpec extends Specification {
                $declaration
            }
        """)
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
