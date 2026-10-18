package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.reporting.RuleContext
import spock.lang.Specification
import spock.lang.Tag

/**
 * Covers each rule's {@code visit} in isolation from the decision it delegates to, which the fixtures
 * cannot do: they drive a rule end to end, so they cannot tell a fault in {@code visit} from a fault
 * in the predicate it calls.
 *
 * Each rule also has a case asserting the returned value. PMD discards a rulechain visitor's return,
 * so nothing observable depends on it, which is why it has to be asserted here rather than exempted
 * from mutation.
 */
@Tag('unit')
class VisitDelegationSpec extends Specification {

    ASTLocalVariableDeclaration declaration = Mock()
    ASTMethodDeclaration method = Mock()
    ASTCompilationUnit unit = Mock()

    def 'useVarForLocalVariables reports a rewritable declaration'() {
        UseVarForLocalVariables rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(declaration, context)

        then:
        1 * rule.isRewritableAsVar(declaration) >> true
        1 * rule.reportIfRewritableAsVar(declaration, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(declaration)
        0 * _
    }

    def 'useVarForLocalVariables reports nothing otherwise'() {
        UseVarForLocalVariables rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(declaration, context)

        then:
        1 * rule.isRewritableAsVar(declaration) >> false
        1 * rule.reportIfRewritableAsVar(declaration, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        0 * _
    }

    def 'useVarForLocalVariables returns the data it was given'() {
        UseVarForLocalVariables rule = Spy()
        RuleContext context = Mock()

        when:
        def result = rule.visit(declaration, context)

        then:
        1 * rule.isRewritableAsVar(declaration) >> false
        1 * rule.reportIfRewritableAsVar(declaration, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        0 * _

        expect:
        result.is(context)
    }

    def 'staticMethodsModifyStaticState reports an unjustified static'() {
        StaticMethodsModifyStaticState rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(method, context)

        then:
        1 * rule.isUnjustifiedStatic(method) >> true
        1 * rule.reportIfUnjustifiedStatic(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(method)
        0 * _
    }

    def 'staticMethodsModifyStaticState reports nothing otherwise'() {
        StaticMethodsModifyStaticState rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(method, context)

        then:
        1 * rule.isUnjustifiedStatic(method) >> false
        1 * rule.reportIfUnjustifiedStatic(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        0 * _
    }

    def 'staticMethodsModifyStaticState returns the data it was given'() {
        StaticMethodsModifyStaticState rule = Spy()
        RuleContext context = Mock()

        when:
        def result = rule.visit(method, context)

        then:
        1 * rule.isUnjustifiedStatic(method) >> false
        1 * rule.reportIfUnjustifiedStatic(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        0 * _

        expect:
        result.is(context)
    }

    def 'avoidPrivateAndProtectedMethods reports a hidden method'() {
        AvoidPrivateAndProtectedMethods rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(method, context)

        then:
        1 * rule.isHidden(method) >> true
        1 * rule.reportIfHidden(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(method)
        0 * _
    }

    def 'avoidPrivateAndProtectedMethods reports nothing otherwise'() {
        AvoidPrivateAndProtectedMethods rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(method, context)

        then:
        1 * rule.isHidden(method) >> false
        1 * rule.reportIfHidden(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        0 * _
    }

    def 'avoidPrivateAndProtectedMethods returns the data it was given'() {
        AvoidPrivateAndProtectedMethods rule = Spy()
        RuleContext context = Mock()

        when:
        def result = rule.visit(method, context)

        then:
        1 * rule.isHidden(method) >> false
        1 * rule.reportIfHidden(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        0 * _

        expect:
        result.is(context)
    }

    def 'useVisibleForTestingAnnotation reports an unmarked seam'() {
        UseVisibleForTestingAnnotation rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(method, context)

        then:
        1 * rule.isUnmarkedSeam(method) >> true
        1 * rule.reportIfUnmarkedSeam(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(method)
        0 * _
    }

    def 'useVisibleForTestingAnnotation reports nothing otherwise'() {
        UseVisibleForTestingAnnotation rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(method, context)

        then:
        1 * rule.isUnmarkedSeam(method) >> false
        1 * rule.reportIfUnmarkedSeam(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        0 * _
    }

    def 'useVisibleForTestingAnnotation returns the data it was given'() {
        UseVisibleForTestingAnnotation rule = Spy()
        RuleContext context = Mock()

        when:
        def result = rule.visit(method, context)

        then:
        1 * rule.isUnmarkedSeam(method) >> false
        1 * rule.reportIfUnmarkedSeam(method, context) >> { callRealMethod() }
        1 * rule._
        _ * context.rule >> rule
        0 * _

        expect:
        result.is(context)
    }

    def 'useStaticImports delegates to its collector'() {
        UseStaticImports rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(unit, context)

        then:
        1 * rule.reportImportableMembers(unit, context) >> {}
        1 * rule._
        _ * context.rule >> rule
        0 * _
    }

    def 'useStaticImports returns the data it was given'() {
        UseStaticImports rule = Spy()
        RuleContext context = Mock()

        when:
        def result = rule.visit(unit, context)

        then:
        1 * rule.reportImportableMembers(unit, context) >> {}
        1 * rule._
        _ * context.rule >> rule
        0 * _

        expect:
        result.is(context)
    }

    def 'useTypeImports delegates to its collector'() {
        UseTypeImports rule = Spy()
        RuleContext context = Mock()

        when:
        rule.visit(unit, context)

        then:
        1 * rule.reportImportableTypes(unit, context) >> {}
        1 * rule._
        _ * context.rule >> rule
        0 * _
    }

    def 'useTypeImports returns the data it was given'() {
        UseTypeImports rule = Spy()
        RuleContext context = Mock()

        when:
        def result = rule.visit(unit, context)

        then:
        1 * rule.reportImportableTypes(unit, context) >> {}
        1 * rule._
        _ * context.rule >> rule
        0 * _

        expect:
        result.is(context)
    }
}
