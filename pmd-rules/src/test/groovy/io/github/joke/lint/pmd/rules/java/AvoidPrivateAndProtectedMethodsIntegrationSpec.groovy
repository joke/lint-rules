package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

/**
 * Smoke check that the rule is wired up and fires through PMD, plus one fact of PMD's model the rule
 * depends on: an annotation written through its outer type is seen by its simple name. The decisions are
 * in {@link AvoidPrivateAndProtectedMethodsSpec}.
 */
@Tag('integration')
class AvoidPrivateAndProtectedMethodsIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('AvoidPrivateAndProtectedMethods', fixture, version) == lines

        where:
        fixture                                                          | version | lines
        'APrivateMethodIsReported'                                       | null    | [2]
        'TheSameMarkerQualifiedThroughItsOuterTypeIsAlsoMatched'         | null    | []
    }
}
