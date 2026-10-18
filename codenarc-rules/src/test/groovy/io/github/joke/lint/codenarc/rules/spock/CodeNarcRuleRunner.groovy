package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Runs one rule over one {@code .groovy} fixture from {@code src/test/resources} through CodeNarc's own
 * {@code applyTo}, and returns the lines of its violations. The integration counterpart of
 * {@code PmdRuleRunner}, with the same shape.
 */
class CodeNarcRuleRunner {

    private static final String PACKAGE = 'io.github.joke.lint.codenarc.rules.spock'
    private static final String FIXTURES = 'io/github/joke/lint/codenarc/rules/spock/fixtures'

    static List<Integer> violationLines(String ruleName, String fixture) {
        Rule rule = (Rule) Class.forName("${PACKAGE}.${ruleName}Rule").newInstance()
        def source = new String(Files.readAllBytes(fixturePath(ruleName, fixture)), 'UTF-8')
        rule.applyTo(new SourceString(source))*.lineNumber
    }

    private static java.nio.file.Path fixturePath(String ruleName, String fixture) {
        def url = CodeNarcRuleRunner.classLoader.getResource("${FIXTURES}/${ruleName}/${fixture}.groovy")
        Paths.get(url.toURI())
    }
}
