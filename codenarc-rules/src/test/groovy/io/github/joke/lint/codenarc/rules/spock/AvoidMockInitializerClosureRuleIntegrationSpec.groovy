package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link AvoidMockInitializerClosureAstVisitorSpec}.
 */
@Tag('integration')
class AvoidMockInitializerClosureRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('AvoidMockInitializerClosure', fixture) == lines

        where:
        fixture                                  | lines
        'ClosureInMockInitializerIsReported' | [4]
        'MockWithoutClosureIsNotReported'    | []
        'DeepMockClosureIsReported'  | [4]
    }
}
