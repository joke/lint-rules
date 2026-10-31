package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

/**
 * Smoke check that the rule is wired up and fires through PMD, plus the one language-level case only
 * real PMD can show. The decisions are in {@link UseVarForLocalVariablesSpec}.
 */
@Tag('integration')
@Subject(UseVarForLocalVariables)
class UseVarForLocalVariablesIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('UseVarForLocalVariables', fixture, version) == lines

        where:
        fixture                                             | version | lines
        'AnExplicitlyTypedLocalVariableIsReported'          | null    | [3]
        'ATryWithResourcesDeclarationIsReported'            | null    | [3]
        'TheRuleAppliesFromJava10'                          | '10'    | [3]
        'TheRuleAppliesFromJava10'                          | '8'     | []
    }

    def 'the targetJavaVersion property decides whether the rule reports'() {
        expect:
        violationLines('UseVarForLocalVariables', 'AnExplicitlyTypedLocalVariableIsReported', null,
                [targetJavaVersion: target]) == lines

        where:
        target | lines
        '8'    | []
        '9'    | []
        '10'   | [3]
        '17'   | [3]
    }

    def 'a targetJavaVersion that is not a number is rejected by PMD'() {
        when:
        violationLines('UseVarForLocalVariables', 'AnExplicitlyTypedLocalVariableIsReported', null,
                [targetJavaVersion: 'eight'])

        then:
        thrown(Exception)
        0 * _
    }
}
