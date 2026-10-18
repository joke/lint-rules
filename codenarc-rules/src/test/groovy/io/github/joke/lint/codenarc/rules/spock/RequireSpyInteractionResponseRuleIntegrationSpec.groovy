package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link RequireSpyInteractionResponseAstVisitorSpec}.
 */
@Tag('integration')
@Subject(RequireSpyInteractionResponseRule)
class RequireSpyInteractionResponseRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('RequireSpyInteractionResponse', fixture) == lines

        where:
        fixture                                            | lines
        'UnansweredCountedInteractionOnASpyIsReported' | [10]
        'AnsweredInteractionOnASpyIsNotReported'       | []
    }
}
