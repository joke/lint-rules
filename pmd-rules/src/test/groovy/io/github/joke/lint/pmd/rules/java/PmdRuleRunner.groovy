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

    static List<RuleViolation> violations(String ruleName, String fixture, String javaVersion = null) {
        def configuration = new PMDConfiguration()
        if (javaVersion) {
            configuration.defaultLanguageVersion = LanguageRegistry.PMD.getLanguageVersionById('java', javaVersion)
        }
        configuration.addInputPath(fixturePath(ruleName, fixture))
        PmdAnalysis.create(configuration).withCloseable { pmd ->
            def rule = pmd.newRuleSetLoader().loadFromResource(CATEGORY).rules.find { it.name == ruleName }
            pmd.addRuleSet(RuleSet.forSingleRule(rule))
            def report = pmd.performAnalysisAndCollectReport()
            if (report.processingErrors || report.configurationErrors) {
                throw new IllegalStateException("PMD reported errors for ${ruleName}/${fixture}: "
                        + "${report.processingErrors} ${report.configurationErrors}")
            }
            report.violations
        }
    }

    static List<Integer> violationLines(String ruleName, String fixture, String javaVersion = null) {
        violations(ruleName, fixture, javaVersion)*.beginLine
    }

    private static Path fixturePath(String ruleName, String fixture) {
        def url = PmdRuleRunner.classLoader.getResource("${FIXTURES}/${ruleName}/${fixture}.java")
        Paths.get(url.toURI())
    }
}
