package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link RequireValidatedInteractionArgumentsAstVisitorSpec}.
 */
@Tag('integration')
@Subject(RequireValidatedInteractionArgumentsRule)
class RequireValidatedInteractionArgumentsRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('RequireValidatedInteractionArguments', fixture) == lines

        where:
        fixture                             | lines
        'WildcardArgumentIsReported'    | [8]
        'ExplicitArgumentIsNotReported' | []
    }
}
