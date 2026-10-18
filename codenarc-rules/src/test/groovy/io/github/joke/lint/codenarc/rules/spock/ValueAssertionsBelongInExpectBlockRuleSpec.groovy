package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * The rule class's contract: its name and priority, the specification gate it carries, and the visitor it
 * names. The reporting decisions are in {@link ValueAssertionsBelongInExpectBlockAstVisitorSpec}.
 */
@Tag('unit')
class ValueAssertionsBelongInExpectBlockRuleSpec extends Specification {

    ValueAssertionsBelongInExpectBlockRule rule = new ValueAssertionsBelongInExpectBlockRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'ValueAssertionsBelongInExpectBlock'
        rule.priority == 2
    }

    def 'the rule names its nested visitor'() {
        expect:
        rule.astVisitorClass == ValueAssertionsBelongInExpectBlockRule.ValueAssertionsBelongInExpectBlockAstVisitor
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
