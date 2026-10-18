package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link InteractionsBelongInThenBlockAstVisitorSpec}.
 */
@Tag('integration')
class InteractionsBelongInThenBlockRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('InteractionsBelongInThenBlock', fixture) == lines

        where:
        fixture                              | lines
        'InteractionInWhenIsReported'    | [7]
        'InteractionInThenIsNotReported' | []
    }
}
