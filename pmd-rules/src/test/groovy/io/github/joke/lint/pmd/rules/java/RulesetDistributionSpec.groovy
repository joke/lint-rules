package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.PMDConfiguration
import net.sourceforge.pmd.PmdAnalysis
import net.sourceforge.pmd.reporting.Report
import spock.lang.Specification
import spock.lang.Tag

import java.nio.file.Files
import java.nio.file.Path

/**
 * Exercises the artifact the way a consuming Gradle build does: the ruleset is loaded by classpath
 * reference from the jar, not by file path, and analysis runs through the API subset that is stable
 * across all of PMD 7.
 *
 * Deliberately does not use {@code pmd-test}. Its {@code RuleTst} is itself versioned, so a failure
 * here would not distinguish a PMD incompatibility from a harness incompatibility, which is the only
 * thing this specification exists to detect. The build runs this class once per supported PMD version.
 */
@Tag('integration')
class RulesetDistributionSpec extends Specification {

    static final String RULESET = 'rulesets/java/joke.xml'
    static final String CATEGORY = 'category/java/joke.xml'

    static List<String> ruleNames(String resource) {
        PmdAnalysis.create(new PMDConfiguration()).withCloseable { pmd ->
            pmd.newRuleSetLoader().loadFromResource(resource).rules*.name
        }
    }

    static Report analyse(Path dir, String name, String source) {
        def file = dir.resolve("${name}.java")
        Files.write(file, [source])
        def configuration = new PMDConfiguration()
        configuration.addInputPath(file)
        PmdAnalysis.create(configuration).withCloseable { pmd ->
            pmd.addRuleSet(pmd.newRuleSetLoader().loadFromResource(RULESET))
            pmd.performAnalysisAndCollectReport()
        }
    }

    def 'the convenience ruleset selects every rule in the category'() {
        expect:
        !ruleNames(RULESET).empty
        ruleNames(RULESET) as Set == ruleNames(CATEGORY) as Set
    }

    def 'the ruleset ships every rule'() {
        expect:
        ruleNames(RULESET).containsAll([
                'UseVarForLocalVariables',
                'StaticMethodsModifyStaticState',
                'AvoidPrivateAndProtectedMethods',
                'UseVisibleForTestingAnnotation',
                'AvoidLambdaBlockBodies',
                'AvoidAnonymousClasses',
                'UseStaticImports',
                'UseTypeImports',
        ])
    }

    def 'a violating source file is reported'() {
        def dir = Files.createTempDirectory('ruleset')

        expect:
        def report = analyse(dir, 'Bad', 'class Bad { public void m() { String name = "joke"; } }')
        report.violations*.rule*.name == ['UseVarForLocalVariables']

        cleanup:
        dir.toFile().deleteDir()
    }

    def 'a compliant source file is not reported'() {
        def dir = Files.createTempDirectory('ruleset')

        expect:
        def report = analyse(dir, 'Good', 'class Good { public void m() { var name = "joke"; } }')
        report.violations.empty

        cleanup:
        dir.toFile().deleteDir()
    }

    def 'the violation carries the configured message'() {
        def dir = Files.createTempDirectory('ruleset')

        expect:
        def report = analyse(dir, 'Bad', 'class Bad { public void m() { String name = "joke"; } }')
        report.violations*.description == ["Use 'var' instead of an explicit local variable type."]

        cleanup:
        dir.toFile().deleteDir()
    }

}
