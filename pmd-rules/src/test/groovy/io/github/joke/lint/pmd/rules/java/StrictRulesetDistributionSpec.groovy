package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.PMDConfiguration
import net.sourceforge.pmd.PmdAnalysis
import spock.lang.Specification
import spock.lang.Tag

/**
 * Exercises {@code rulesets/java/joke-strict.xml}, the one shipped resource that names rules this
 * artifact does not define. Loading it is the whole point: a stock rule PMD renamed or removed is a
 * ruleset-load failure, and this specification is where that surfaces, on a build here rather than in
 * a consumer's analysis.
 *
 * Separate from {@link RulesetDistributionSpec} so that a failure names which resource broke: the two
 * carry different PMD floors, and one class covering both would not say which one the build was
 * complaining about.
 *
 * Deliberately does not use {@code pmd-test}, for the reason given on {@link RulesetDistributionSpec}.
 */
@Tag('integration')
class StrictRulesetDistributionSpec extends Specification {

    static final String STRICT = 'rulesets/java/joke-strict.xml'
    static final String CATEGORY = 'category/java/joke.xml'

    static List<String> ruleNames(String resource) {
        PmdAnalysis.create(new PMDConfiguration()).withCloseable { pmd ->
            pmd.newRuleSetLoader().loadFromResource(resource).rules*.name
        }
    }

    static Object intProperty(String ruleName, String propertyName) {
        PmdAnalysis.create(new PMDConfiguration()).withCloseable { pmd ->
            def rule = pmd.newRuleSetLoader().loadFromResource(STRICT).rules.find { it.name == ruleName }
            assert rule : "${ruleName} is not in ${STRICT}"
            def descriptor = rule.getPropertyDescriptor(propertyName)
            assert descriptor : "${ruleName} has no property ${propertyName}"
            rule.getProperty(descriptor)
        }
    }

    def 'the strict ruleset resolves every rule it names'() {
        expect:
        !ruleNames(STRICT).empty
    }

    def 'the strict ruleset contains every rule this artifact defines'() {
        expect:
        ruleNames(STRICT).containsAll(ruleNames(CATEGORY))
    }

    def 'the strict ruleset contains rules this artifact does not define'() {
        expect:
        ruleNames(STRICT).size() > ruleNames(CATEGORY).size()
    }

    def 'the static import cap is excluded'() {
        expect:
        ruleNames(STRICT).contains('UseStaticImports')
        !ruleNames(STRICT).contains('TooManyStaticImports')
    }

    def 'the cognitive complexity override survives the composition'() {
        expect:
        intProperty('CognitiveComplexity', 'reportLevel') == 5
    }

    def 'the NPath complexity override survives the composition'() {
        expect:
        intProperty('NPathComplexity', 'reportLevel') == 5
    }

    def 'the nested if depth override survives the composition'() {
        expect:
        intProperty('AvoidDeeplyNestedIfStmts', 'problemDepth') == 2
    }

}
