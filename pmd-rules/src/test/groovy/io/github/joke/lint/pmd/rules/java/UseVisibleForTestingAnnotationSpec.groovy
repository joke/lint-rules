package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.ast.NodeStream
import net.sourceforge.pmd.lang.java.ast.ASTAnnotation
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.ast.ModifierOwner.Visibility
import net.sourceforge.pmd.reporting.RuleContext
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

import java.util.function.Predicate

/**
 * Each {@code @VisibleForTesting} method of {@link UseVisibleForTestingAnnotation} is tested directly. The
 * method under test is the spy's real method; the siblings it calls are stubbed, so each feature sees one
 * decision.
 */
@Tag('unit')
@Subject(UseVisibleForTestingAnnotation)
class UseVisibleForTestingAnnotationSpec extends Specification {

    ASTMethodDeclaration method = Mock()
    RuleContext context = Mock()

    def 'visit delegates to the report decision and returns the data it was given'() {
        UseVisibleForTestingAnnotation rule = Spy()

        when:
        def result = rule.visit(method, context)

        then:
        1 * rule.reportIfUnmarkedSeam(method, context) >> {}
        1 * rule._
        0 * _

        expect:
        result.is(context)
    }

    def 'reportIfUnmarkedSeam adds a violation for an unmarked seam'() {
        UseVisibleForTestingAnnotation rule = Spy()

        when:
        rule.reportIfUnmarkedSeam(method, context)

        then:
        1 * rule.isUnmarkedSeam(method) >> true
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(method)
        0 * _
    }

    def 'reportIfUnmarkedSeam adds no violation otherwise'() {
        UseVisibleForTestingAnnotation rule = Spy()

        when:
        rule.reportIfUnmarkedSeam(method, context)

        then:
        1 * rule.isUnmarkedSeam(method) >> false
        1 * rule._
        0 * _
    }

    def 'isUnmarkedSeam is false for a method that is not package-private'() {
        UseVisibleForTestingAnnotation rule = new UseVisibleForTestingAnnotation()

        when:
        def result = rule.isUnmarkedSeam(method)

        then:
        1 * method.visibility >> Visibility.V_PUBLIC
        0 * _

        expect:
        !result
    }

    def 'isUnmarkedSeam is true for a package-private method with no annotation'() {
        UseVisibleForTestingAnnotation rule = new UseVisibleForTestingAnnotation()

        when:
        def result = rule.isUnmarkedSeam(method)

        then:
        1 * method.visibility >> Visibility.V_PACKAGE
        1 * method.declaredAnnotations >> annotations([])
        0 * _

        expect:
        result
    }

    def 'isUnmarkedSeam is decided by whether the one annotation is an intentional marker'() {
        UseVisibleForTestingAnnotation rule = new UseVisibleForTestingAnnotation()
        ASTAnnotation annotation = Mock()

        when:
        def result = rule.isUnmarkedSeam(method)

        then:
        1 * method.visibility >> Visibility.V_PACKAGE
        1 * method.declaredAnnotations >> annotations([annotation])
        1 * annotation.simpleName >> simpleName
        0 * _

        expect:
        result == expected

        where:
        simpleName   | expected
        'Test'       | false
        'Deprecated' | true
    }

    def 'hasAnyAnnotation is true when an annotation has one of the given simple names'() {
        UseVisibleForTestingAnnotation rule = new UseVisibleForTestingAnnotation()
        ASTAnnotation annotation = Mock()

        when:
        def result = rule.hasAnyAnnotation(method, ['Test', 'Override'] as Set)

        then:
        1 * method.declaredAnnotations >> annotations([annotation])
        1 * annotation.simpleName >> 'Override'
        0 * _

        expect:
        result
    }

    def 'hasAnyAnnotation is false when no annotation has one of the given simple names'() {
        UseVisibleForTestingAnnotation rule = new UseVisibleForTestingAnnotation()
        ASTAnnotation annotation = Mock()

        when:
        def result = rule.hasAnyAnnotation(method, ['Test'] as Set)

        then:
        1 * method.declaredAnnotations >> annotations([annotation])
        1 * annotation.simpleName >> 'Deprecated'
        0 * _

        expect:
        !result
    }

    /**
     * A stream of the given annotations whose {@code any} runs the predicate it is given, so the rule's
     * own lambda is exercised rather than replaced.
     */
    private static NodeStream annotations(List annotations) {
        [any: { Predicate matches -> annotations.any { matches.test(it) } }] as NodeStream
    }
}
