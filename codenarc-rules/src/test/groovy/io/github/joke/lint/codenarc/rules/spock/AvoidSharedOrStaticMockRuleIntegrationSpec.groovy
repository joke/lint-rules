package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link AvoidSharedOrStaticMockAstVisitorSpec}.
 */
@Tag('integration')
class AvoidSharedOrStaticMockRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('AvoidSharedOrStaticMock', fixture) == lines

        where:
        fixture                         | lines
        'SharedMockIsReported'      | [6]
        'StaticMockIsReported'      | [4]
        'InstanceMockIsNotReported' | []
        'SharedDeepMockIsReported'| [6]
    }
}
