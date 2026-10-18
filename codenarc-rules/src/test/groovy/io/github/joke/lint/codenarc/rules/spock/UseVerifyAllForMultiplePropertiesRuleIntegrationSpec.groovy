package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link UseVerifyAllForMultiplePropertiesAstVisitorSpec}.
 */
@Tag('integration')
class UseVerifyAllForMultiplePropertiesRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('UseVerifyAllForMultipleProperties', fixture) == lines

        where:
        fixture                                      | lines
        'SeveralConditionsInAClosureAreReported' | [8]
        'SingleConditionIsNotReported'           | []
    }
}
