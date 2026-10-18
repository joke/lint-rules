package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link RequireSpyEntryInteractionAstVisitorSpec}.
 */
@Tag('integration')
class RequireSpyEntryInteractionRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('RequireSpyEntryInteraction', fixture) == lines

        where:
        fixture                              | lines
        'EntryMissingIsReported'         | [10]
        'EntryDeclaredFirstIsReported'   | [10]
        'EntryDeclaredLastIsNotReported' | []
    }
}
