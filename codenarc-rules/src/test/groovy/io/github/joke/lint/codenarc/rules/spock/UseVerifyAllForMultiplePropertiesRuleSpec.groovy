package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * The rule class's contract: its name and priority, the specification gate it carries, and the visitor it
 * names. The reporting decisions are in {@link UseVerifyAllForMultiplePropertiesAstVisitorSpec}.
 */
@Tag('unit')
@Subject(UseVerifyAllForMultiplePropertiesRule)
class UseVerifyAllForMultiplePropertiesRuleSpec extends Specification {

    UseVerifyAllForMultiplePropertiesRule rule = new UseVerifyAllForMultiplePropertiesRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'UseVerifyAllForMultipleProperties'
        rule.priority == 2
    }

    def 'the rule names its nested visitor'() {
        expect:
        rule.astVisitorClass == UseVerifyAllForMultiplePropertiesRule.UseVerifyAllForMultiplePropertiesAstVisitor
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
}
