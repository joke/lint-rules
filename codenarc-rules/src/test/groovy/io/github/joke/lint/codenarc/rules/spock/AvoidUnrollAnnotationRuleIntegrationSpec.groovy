package io.github.joke.lint.codenarc.rules.spock

import spock.lang.Specification
import spock.lang.Tag

/**
 * Smoke check that the rule is wired up and fires through CodeNarc, plus the name-matching fact the rule
 * depends on: a qualified annotation is seen by its qualified name. The decisions are in
 * {@link AvoidUnrollAnnotationAstVisitorSpec}.
 */
@Tag('integration')
class AvoidUnrollAnnotationRuleIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        CodeNarcRuleRunner.violationLines('AvoidUnrollAnnotation', fixture) == lines

        where:
        fixture                              | lines
        'AnnotatedFeatureIsReported'         | [4]
        'FullyQualifiedAnnotationIsReported' | [4]
        'PlainClassIsNotReported'            | []
    }
}
