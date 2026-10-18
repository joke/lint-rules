package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link RequireStrictMockingTerminatorAstVisitorSpec}.
 */
@Tag('integration')
class RequireStrictMockingTerminatorRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('RequireStrictMockingTerminator', fixture) == lines

        where:
        fixture                           | lines
        'MissingTerminatorIsReported' | [8]
        'TerminatorIsNotReported'     | []
    }
}
