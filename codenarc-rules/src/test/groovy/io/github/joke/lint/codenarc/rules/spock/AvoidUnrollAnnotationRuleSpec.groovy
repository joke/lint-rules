package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * The rule class's contract: its name and priority, the specification gate it carries, and the visitor it
 * names. The reporting decisions are in {@link AvoidUnrollAnnotationAstVisitorSpec}.
 */
@Tag('unit')
class AvoidUnrollAnnotationRuleSpec extends Specification {

    AvoidUnrollAnnotationRule rule = new AvoidUnrollAnnotationRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'AvoidUnrollAnnotation'
        rule.priority == 2
    }

    def 'the rule names its nested visitor'() {
        expect:
        rule.astVisitorClass == AvoidUnrollAnnotationRule.AvoidUnrollAnnotationAstVisitor
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
}
