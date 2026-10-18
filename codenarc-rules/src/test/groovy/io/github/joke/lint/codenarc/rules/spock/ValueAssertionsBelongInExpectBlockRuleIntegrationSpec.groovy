package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc, plus the fact of Spock's own vocabulary
 * the rule must leave alone: {@code thrown(...)} is an exception check, not a value assertion. The decisions
 * are in {@link ValueAssertionsBelongInExpectBlockAstVisitorSpec}.
 */
@Tag('integration')
@Subject(ValueAssertionsBelongInExpectBlockRule)
class ValueAssertionsBelongInExpectBlockRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('ValueAssertionsBelongInExpectBlock', fixture) == lines

        where:
        fixture                              | lines
        'ValueAssertionInThenIsReported'     | [8]
        'ThrownIsNotReported'                | []
    }
}
