package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class AvoidUnrollAnnotationRuleSpec extends Specification {

    Rule rule = new AvoidUnrollAnnotationRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'AvoidUnrollAnnotation'
        rule.priority == 2
    }

    def 'the specification gate defaults to any Specification subclass'() {
        expect:
        rule.specificationSuperclassNames == '*Specification'
        rule.specificationClassNames == null
    }

    def 'the name and priority are settable, as CodeNarc ruleset configuration requires'() {
        when:
        rule.name = 'Renamed'
        rule.priority = 3

        then:
        0 * _

        expect:
        rule.name == 'Renamed'
        rule.priority == 3
    }

    def 'the gate properties are settable, as CodeNarc ruleset configuration requires'() {
        when:
        rule.specificationSuperclassNames = '*Base'
        rule.specificationClassNames = '*Example'

        then:
        0 * _

        expect:
        rule.specificationSuperclassNames == '*Base'
        rule.specificationClassNames == '*Example'
    }

    def 'an annotated feature method is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                @Unroll
                def 'a feature'() { }
            }
        ''')*.message == ['Spock 2 unrolls by default, so @Unroll adds nothing.']
    }

    def 'a fully qualified annotation is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                @spock.lang.Unroll
                def 'a feature'() { }
            }
        ''').size() == 1
    }

    def 'an annotated specification class is reported'() {
        expect:
        violationsIn('''
            @Unroll
            class ExampleSpec extends Specification {
                def 'a feature'() { }
            }
        ''').size() == 1
    }

    def 'an unannotated specification is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() { }
            }
        ''').empty
    }

    def 'an unrelated annotation is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                @Override
                def 'a feature'() { }
            }
        ''').empty
    }

    def 'an annotation whose simple name merely ends in Unroll is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                @NotUnroll
                def 'a feature'() { }
            }
        ''').empty
    }

    def 'both a class and a method annotation are reported'() {
        expect:
        violationsIn('''
            @Unroll
            class ExampleSpec extends Specification {
                @Unroll
                def 'a feature'() { }
            }
        ''').size() == 2
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            @Unroll
            class PlainClass {
                @Unroll
                def 'a method'() { }
            }
        ''').empty
    }

    def 'specificationClassNames widens the gate to a class that extends nothing'() {
        rule.specificationClassNames = 'PlainClass'

        expect:
        violationsIn('''
            class PlainClass {
                @Unroll
                def 'a method'() { }
            }
        ''').size() == 1
    }

    def 'specificationSuperclassNames retargets the gate at another base class'() {
        rule.specificationSuperclassNames = 'CustomBase'

        expect:
        violationsIn('''
            class ExampleSpec extends CustomBase {
                @Unroll
                def 'a feature'() { }
            }
        ''').size() == 1
    }

    def 'doNotApplyToClassNames still excludes a specification, as it does for any CodeNarc rule'() {
        rule.doNotApplyToClassNames = 'ExampleSpec'

        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                @Unroll
                def 'a feature'() { }
            }
        ''').empty
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
