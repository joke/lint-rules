package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

/**
 * Smoke check that the rule is wired up and fires through PMD. The decisions are in
 * {@link AvoidLambdaBlockBodiesSpec}.
 */
@Tag('integration')
@Subject(AvoidLambdaBlockBodies)
class AvoidLambdaBlockBodiesIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('AvoidLambdaBlockBodies', fixture, version) == lines

        where:
        fixture                                                                   | version | lines
        'AMultiStatementBlockBodyIsReported'                                      | null    | [3]
        'ABlockContainingOnlyACommentIsNotReportedACommentIsNotAStatement'        | null    | []
        'TheRuleAppliesFromJava8'                                                 | '8'     | [3]
    }
}
