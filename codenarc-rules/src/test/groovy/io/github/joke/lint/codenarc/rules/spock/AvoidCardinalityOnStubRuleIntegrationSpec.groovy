package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link AvoidCardinalityOnStubAstVisitorSpec}.
 */
@Tag('integration')
class AvoidCardinalityOnStubRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('AvoidCardinalityOnStub', fixture) == lines

        where:
        fixture                                   | lines
        'CountedInteractionOnAStubIsReported' | [10]
        'StubbedInteractionIsNotReported'     | []
    }
}
