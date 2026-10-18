package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link DeclareMockWithExplicitTypeAstVisitorSpec}.
 */
@Tag('integration')
class DeclareMockWithExplicitTypeRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('DeclareMockWithExplicitType', fixture) == lines

        where:
        fixture                      | lines
        'DynamicMockIsReported'  | [5]
        'RepeatedTypeIsReported' | [5]
        'TypedMockIsNotReported' | []
        'DeepMockRepeatingTypeIsReported'| [5]
    }
}
