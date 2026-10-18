package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.ast.NodeStream
import net.sourceforge.pmd.lang.java.ast.ASTAnnotation
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.ast.Annotatable
import net.sourceforge.pmd.lang.java.ast.ModifierOwner.Visibility
import net.sourceforge.pmd.reporting.RuleContext
import spock.lang.Specification
import spock.lang.Tag

import java.util.function.Predicate

/**
 * Each {@code @VisibleForTesting} method of {@link AvoidPrivateAndProtectedMethods} is tested directly. The
 * method under test is the spy's real method; the siblings it calls are stubbed, so each feature sees one
 * decision.
 */
@Tag('unit')
class AvoidPrivateAndProtectedMethodsSpec extends Specification {

    ASTMethodDeclaration method = Mock()
    RuleContext context = Mock()

    def 'visit delegates to the report decision and returns the data it was given'() {
        AvoidPrivateAndProtectedMethods rule = Spy()

        when:
        def result = rule.visit(method, context)

        then:
        1 * rule.reportIfHidden(method, context) >> {}
        1 * rule._
        0 * _

        expect:
        result.is(context)
    }

    def 'reportIfHidden adds a violation for a hidden method'() {
        AvoidPrivateAndProtectedMethods rule = Spy()

        when:
        rule.reportIfHidden(method, context)

        then:
        1 * rule.isHidden(method) >> true
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(method)
        0 * _
    }

    def 'reportIfHidden adds no violation otherwise'() {
        AvoidPrivateAndProtectedMethods rule = Spy()

        when:
        rule.reportIfHidden(method, context)

        then:
        1 * rule.isHidden(method) >> false
        1 * rule._
        0 * _
    }

    def 'isHidden needs a private or protected method that is neither fixed elsewhere nor declared protected intent'() {
        AvoidPrivateAndProtectedMethods rule = Spy()

        when:
        def result = rule.isHidden(method)

        then:
        1 * method.visibility >> visibility
        (visibility in [Visibility.V_PRIVATE, Visibility.V_PROTECTED] ? 1 : 0) * rule.isVisibilityFixedElsewhere(method) >> fixedElsewhere
        (visibility in [Visibility.V_PRIVATE, Visibility.V_PROTECTED] && !fixedElsewhere ? 1 : 0) * rule.isDeclaredProtectedIntent(method) >> protectedIntent
        1 * rule._
        0 * _

        expect:
        result == expected

        where:
        visibility             | fixedElsewhere | protectedIntent | expected
        Visibility.V_PUBLIC    | false          | false           | false
        Visibility.V_PACKAGE   | false          | false           | false
        Visibility.V_PRIVATE   | true           | false           | false
        Visibility.V_PRIVATE   | false          | false           | true
        Visibility.V_PROTECTED | false          | true            | false
        Visibility.V_PROTECTED | false          | false           | true
    }

    def 'isVisibilityFixedElsewhere is true for a static method, without looking at annotations'() {
        AvoidPrivateAndProtectedMethods rule = Spy()

        when:
        def result = rule.isVisibilityFixedElsewhere(method)

        then:
        1 * method.static >> true
        1 * rule._
        0 * _

        expect:
        result
    }

    def 'isVisibilityFixedElsewhere is decided by an override annotation for an instance method'() {
        AvoidPrivateAndProtectedMethods rule = Spy()

        when:
        def result = rule.isVisibilityFixedElsewhere(method)

        then:
        1 * method.static >> false
        1 * rule.hasAnyAnnotation(method, ['Override'] as Set) >> overridden
        1 * rule._
        0 * _

        expect:
        result == overridden

        where:
        overridden << [true, false]
    }

    def 'isDeclaredProtectedIntent is true for a protected method with a marker'() {
        AvoidPrivateAndProtectedMethods rule = Spy()

        when:
        def result = rule.isDeclaredProtectedIntent(method)

        then:
        1 * method.visibility >> Visibility.V_PROTECTED
        1 * rule.hasAnyAnnotation(method, ['OverrideOnly', 'VisibleForTesting'] as Set) >> marked
        1 * rule._
        0 * _

        expect:
        result == marked

        where:
        marked << [true, false]
    }

    def 'isDeclaredProtectedIntent is false for a private method, whatever its annotations'() {
        AvoidPrivateAndProtectedMethods rule = Spy()

        when:
        def result = rule.isDeclaredProtectedIntent(method)

        then:
        1 * method.visibility >> Visibility.V_PRIVATE
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'hasAnyAnnotation is true when an annotation has one of the given simple names'() {
        AvoidPrivateAndProtectedMethods rule = new AvoidPrivateAndProtectedMethods()
        Annotatable node = Mock(ASTMethodDeclaration)
        ASTAnnotation annotation = Mock()

        when:
        def result = rule.hasAnyAnnotation(node, ['OverrideOnly', 'VisibleForTesting'] as Set)

        then:
        1 * node.declaredAnnotations >> stream([annotation])
        1 * annotation.simpleName >> 'VisibleForTesting'
        0 * _

        expect:
        result
    }

    def 'hasAnyAnnotation is false when no annotation has one of the given simple names'() {
        AvoidPrivateAndProtectedMethods rule = new AvoidPrivateAndProtectedMethods()
        Annotatable node = Mock(ASTMethodDeclaration)
        ASTAnnotation annotation = Mock()

        when:
        def result = rule.hasAnyAnnotation(node, ['OverrideOnly', 'VisibleForTesting'] as Set)

        then:
        1 * node.declaredAnnotations >> stream([annotation])
        1 * annotation.simpleName >> 'Deprecated'
        0 * _

        expect:
        !result
    }

    /**
     * A stream over the given items whose operations run the predicate or class they are given, so the
     * rule's own lambdas are exercised rather than replaced.
     */
    private static NodeStream stream(List items) {
        [
                any: { Predicate matches -> items.any { matches.test(it) } },
        ] as NodeStream
    }
}
