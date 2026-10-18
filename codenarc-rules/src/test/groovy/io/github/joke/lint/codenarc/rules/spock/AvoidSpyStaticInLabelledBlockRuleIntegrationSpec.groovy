package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc. The decisions are in
 * {@link AvoidSpyStaticInLabelledBlockAstVisitorSpec}.
 */
@Tag('integration')
@Subject(AvoidSpyStaticInLabelledBlockRule)
class AvoidSpyStaticInLabelledBlockRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('AvoidSpyStaticInLabelledBlock', fixture) == lines

        where:
        fixture                                       | lines
        'SpyStaticInLabelledBlockIsReported'      | [6]
        'SpyStaticInUnlabelledSetupIsNotReported' | []
    }
}
