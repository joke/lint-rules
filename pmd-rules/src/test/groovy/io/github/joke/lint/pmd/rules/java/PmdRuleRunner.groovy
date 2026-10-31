package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.PMDConfiguration
import net.sourceforge.pmd.PmdAnalysis
import net.sourceforge.pmd.lang.LanguageRegistry
import net.sourceforge.pmd.lang.rule.RuleSet
import net.sourceforge.pmd.reporting.RuleViolation

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Runs one rule of the category against one fixture through PMD's stable API, the way the
 * integration tests do, instead of through {@code pmd-test}'s {@code RuleTst}.
 */
class PmdRuleRunner {

    private static final String CATEGORY = 'category/java/joke.xml'
    private static final String FIXTURES = 'io/github/joke/lint/pmd/rules/java/fixtures'

    /**
     * With {@code properties} empty the rule comes straight from the category. With properties it is
     * referenced from a ruleset XML that sets them, which is the path a consumer's own ruleset takes.
     */
    static List<RuleViolation> violations(String ruleName, String fixture, String javaVersion = null,
                                          Map<String, String> properties = [:]) {
        def configuration = new PMDConfiguration()
        if (javaVersion) {
            configuration.defaultLanguageVersion = LanguageRegistry.PMD.getLanguageVersionById('java', javaVersion)
        }
        configuration.addInputPath(fixturePath(ruleName, fixture))
        PmdAnalysis.create(configuration).withCloseable { pmd ->
            def loader = pmd.newRuleSetLoader()
            def ruleSet = properties
                    ? loader.loadFromString("${ruleName}.xml", rulesetWith(ruleName, properties))
                    : loader.loadFromResource(CATEGORY)
            def rule = ruleSet.rules.find { it.name == ruleName }
            pmd.addRuleSet(RuleSet.forSingleRule(rule))
            def report = pmd.performAnalysisAndCollectReport()
            if (report.processingErrors || report.configurationErrors) {
                throw new IllegalStateException("PMD reported errors for ${ruleName}/${fixture}: "
                        + "${report.processingErrors} ${report.configurationErrors}")
            }
            report.violations
        }
    }

    static List<Integer> violationLines(String ruleName, String fixture, String javaVersion = null,
                                        Map<String, String> properties = [:]) {
        violations(ruleName, fixture, javaVersion, properties)*.beginLine
    }

    private static String rulesetWith(String ruleName, Map<String, String> properties) {
        def propertyXml = properties.collect { name, value -> "<property name=\"${name}\" value=\"${value}\"/>" }.join('')
        """<?xml version="1.0"?>
<ruleset name="integration" xmlns="http://pmd.sourceforge.net/ruleset/2.0.0">
    <description>Integration ruleset for ${ruleName}</description>
    <rule ref="${CATEGORY}/${ruleName}">
        <properties>${propertyXml}</properties>
    </rule>
</ruleset>"""
    }

    private static Path fixturePath(String ruleName, String fixture) {
        def url = PmdRuleRunner.classLoader.getResource("${FIXTURES}/${ruleName}/${fixture}.java")
        Paths.get(url.toURI())
    }
}
