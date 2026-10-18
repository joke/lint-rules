package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.java.ast.ASTAnonymousClassDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTClassBody
import net.sourceforge.pmd.lang.java.ast.ASTEnumConstant
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.reporting.RuleContext
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each {@code @VisibleForTesting} method of {@link AvoidAnonymousClasses} is tested directly. The method
 * under test is the spy's real method; the siblings it calls are stubbed, so each feature sees one
 * decision.
 */
@Tag('unit')
class AvoidAnonymousClassesSpec extends Specification {

    ASTAnonymousClassDeclaration anonymousClass = Mock()
    RuleContext context = Mock()

    def 'visit delegates to the report decision and returns the data it was given'() {
        AvoidAnonymousClasses rule = Spy()

        when:
        def result = rule.visit(anonymousClass, context)

        then:
        1 * rule.reportIfBodyHoldsLogic(anonymousClass, context) >> {}
        1 * rule._
        0 * _

        expect:
        result.is(context)
    }

    def 'reportIfBodyHoldsLogic adds a violation when the body holds logic'() {
        AvoidAnonymousClasses rule = Spy()

        when:
        rule.reportIfBodyHoldsLogic(anonymousClass, context)

        then:
        1 * rule.hasExtractableBody(anonymousClass) >> true
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(anonymousClass)
        0 * _
    }

    def 'reportIfBodyHoldsLogic adds no violation otherwise'() {
        AvoidAnonymousClasses rule = Spy()

        when:
        rule.reportIfBodyHoldsLogic(anonymousClass, context)

        then:
        1 * rule.hasExtractableBody(anonymousClass) >> false
        1 * rule._
        0 * _
    }

    def 'hasExtractableBody is false for an enum constant body, without reading the body'() {
        AvoidAnonymousClasses rule = Spy()

        when:
        def result = rule.hasExtractableBody(anonymousClass)

        then:
        1 * rule.isEnumConstantBody(anonymousClass) >> true
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'hasExtractableBody is false for an empty body'() {
        AvoidAnonymousClasses rule = Spy()
        ASTClassBody body = Mock()

        when:
        def result = rule.hasExtractableBody(anonymousClass)

        then:
        1 * rule.isEnumConstantBody(anonymousClass) >> false
        1 * anonymousClass.body >> body
        1 * body.empty >> true
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'hasExtractableBody is true for a non-empty body that is not an enum constant'() {
        AvoidAnonymousClasses rule = Spy()
        ASTClassBody body = Mock()

        when:
        def result = rule.hasExtractableBody(anonymousClass)

        then:
        1 * rule.isEnumConstantBody(anonymousClass) >> false
        1 * anonymousClass.body >> body
        1 * body.empty >> false
        1 * rule._
        0 * _

        expect:
        result
    }

    def 'isEnumConstantBody is true when the parent is an enum constant'() {
        AvoidAnonymousClasses rule = new AvoidAnonymousClasses()
        ASTEnumConstant enumConstant = Mock()

        when:
        def result = rule.isEnumConstantBody(anonymousClass)

        then:
        1 * anonymousClass.parent >> enumConstant
        0 * _

        expect:
        result
    }

    def 'isEnumConstantBody is false for any other parent'() {
        AvoidAnonymousClasses rule = new AvoidAnonymousClasses()
        ASTMethodDeclaration parent = Mock()

        when:
        def result = rule.isEnumConstantBody(anonymousClass)

        then:
        1 * anonymousClass.parent >> parent
        0 * _

        expect:
        !result
    }
}
