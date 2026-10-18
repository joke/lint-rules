package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link AvoidSetupAndGivenLabelsAstVisitorSpec}.
 */
@Tag('integration')
@Subject(AvoidSetupAndGivenLabelsRule)
class AvoidSetupAndGivenLabelsRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('AvoidSetupAndGivenLabels', fixture) == lines

        where:
        fixture                            | lines
        'SetupLabelIsReported'             | [6]
        'GivenLabelIsReported'             | [6]
        'UnlabelledSetupIsNotReported'     | []
    }
}
