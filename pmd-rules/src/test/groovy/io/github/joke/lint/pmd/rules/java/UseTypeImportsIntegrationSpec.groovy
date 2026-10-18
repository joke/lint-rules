package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

/**
 * Smoke check that the rule is wired up and fires through PMD, plus two facts of PMD's model the rule
 * depends on. The decisions are in {@link UseTypeImportsSpec}.
 */
@Tag('integration')
@Subject(UseTypeImports)
class UseTypeImportsIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('UseTypeImports', fixture, version) == lines

        where:
        fixture                                                | version | lines
        'AFullyQualifiedTypeInADeclarationIsReported'          | null    | [2]
        'AQualifiedNestedTypeIsReported'                       | null    | [2]
        'AnImportDeclarationIsNotItselfReported'               | null    | []
    }
}
