package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.ast.NodeStream
import net.sourceforge.pmd.lang.java.ast.ASTArrayInitializer
import net.sourceforge.pmd.lang.java.ast.ASTBlock
import net.sourceforge.pmd.lang.java.ast.ASTExpression
import net.sourceforge.pmd.lang.java.ast.ASTForeachStatement
import net.sourceforge.pmd.lang.java.ast.ASTLambdaExpression
import net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTMethodReference
import net.sourceforge.pmd.lang.java.ast.ASTNullLiteral
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator
import net.sourceforge.pmd.reporting.RuleContext
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each {@code @VisibleForTesting} method of {@link UseVarForLocalVariables} is tested directly. The method
 * under test is the spy's real method; the siblings it calls are stubbed, so each feature sees one
 * decision.
 */
@Tag('unit')
class UseVarForLocalVariablesSpec extends Specification {

    ASTLocalVariableDeclaration declaration = Mock()
    RuleContext context = Mock()

    def 'visit delegates to the report decision and returns the data it was given'() {
        UseVarForLocalVariables rule = Spy()

        when:
        def result = rule.visit(declaration, context)

        then:
        1 * rule.reportIfRewritableAsVar(declaration, context) >> {}
        1 * rule._
        0 * _

        expect:
        result.is(context)
    }

    def 'reportIfRewritableAsVar adds a violation for a rewritable declaration'() {
        UseVarForLocalVariables rule = Spy()

        when:
        rule.reportIfRewritableAsVar(declaration, context)

        then:
        1 * rule.isRewritableAsVar(declaration) >> true
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(declaration)
        0 * _
    }

    def 'reportIfRewritableAsVar adds no violation otherwise'() {
        UseVarForLocalVariables rule = Spy()

        when:
        rule.reportIfRewritableAsVar(declaration, context)

        then:
        1 * rule.isRewritableAsVar(declaration) >> false
        1 * rule._
        0 * _
    }

    def 'isRewritableAsVar is false for a declaration whose type is already inferred'() {
        UseVarForLocalVariables rule = Spy()

        when:
        def result = rule.isRewritableAsVar(declaration)

        then:
        1 * declaration.typeInferred >> true
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'isRewritableAsVar is false for several declarators, which var forbids'() {
        UseVarForLocalVariables rule = Spy()
        NodeStream<ASTVariableDeclarator> declarators = Mock()

        when:
        def result = rule.isRewritableAsVar(declaration)

        then:
        1 * declaration.typeInferred >> false
        1 * declaration.children(ASTVariableDeclarator) >> declarators
        1 * declarators.toList() >> [Mock(ASTVariableDeclarator), Mock(ASTVariableDeclarator)]
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'isRewritableAsVar is false for a declaration with no declarator'() {
        UseVarForLocalVariables rule = Spy()
        NodeStream<ASTVariableDeclarator> declarators = Mock()

        when:
        def result = rule.isRewritableAsVar(declaration)

        then:
        1 * declaration.typeInferred >> false
        1 * declaration.children(ASTVariableDeclarator) >> declarators
        1 * declarators.toList() >> []
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'isRewritableAsVar is true for one declarator whose type is inferable'() {
        UseVarForLocalVariables rule = Spy()
        NodeStream<ASTVariableDeclarator> declarators = Mock()
        ASTVariableDeclarator declarator = Mock()

        when:
        def result = rule.isRewritableAsVar(declaration)

        then:
        1 * declaration.typeInferred >> false
        1 * declaration.children(ASTVariableDeclarator) >> declarators
        1 * declarators.toList() >> [declarator]
        1 * rule.declaresAnInferableType(declaration, declarator) >> true
        1 * rule._
        0 * _

        expect:
        result
    }

    def 'isRewritableAsVar is false for one declarator whose type is not inferable'() {
        UseVarForLocalVariables rule = Spy()
        NodeStream<ASTVariableDeclarator> declarators = Mock()
        ASTVariableDeclarator declarator = Mock()

        when:
        def result = rule.isRewritableAsVar(declaration)

        then:
        1 * declaration.typeInferred >> false
        1 * declaration.children(ASTVariableDeclarator) >> declarators
        1 * declarators.toList() >> [declarator]
        1 * rule.declaresAnInferableType(declaration, declarator) >> false
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'declaresAnInferableType is true for an enhanced for loop variable, which has no initializer'() {
        UseVarForLocalVariables rule = new UseVarForLocalVariables()
        ASTVariableDeclarator declarator = Mock()

        when:
        def result = rule.declaresAnInferableType(declaration, declarator)

        then:
        1 * declaration.parent >> Mock(ASTForeachStatement)
        0 * _

        expect:
        result
    }

    def 'declaresAnInferableType asks the initializer for any other declaration'() {
        UseVarForLocalVariables rule = Spy()
        ASTVariableDeclarator declarator = Mock()
        ASTExpression initializer = Mock()

        when:
        def result = rule.declaresAnInferableType(declaration, declarator)

        then:
        1 * declaration.parent >> Mock(ASTBlock)
        1 * declarator.initializer >> initializer
        1 * rule.isInferableInitializer(initializer) >> true
        1 * rule._
        0 * _

        expect:
        result
    }

    def 'declaresAnInferableType is false for a non-inferable initializer outside a for loop'() {
        UseVarForLocalVariables rule = Spy()
        ASTVariableDeclarator declarator = Mock()
        ASTExpression initializer = Mock()

        when:
        def result = rule.declaresAnInferableType(declaration, declarator)

        then:
        1 * declaration.parent >> Mock(ASTBlock)
        1 * declarator.initializer >> initializer
        1 * rule.isInferableInitializer(initializer) >> false
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'isInferableInitializer is false when there is no initializer'() {
        UseVarForLocalVariables rule = new UseVarForLocalVariables()

        expect:
        !rule.isInferableInitializer(null)
    }

    def 'isInferableInitializer is false for an initializer var cannot infer a type from'() {
        UseVarForLocalVariables rule = new UseVarForLocalVariables()
        ASTExpression initializer = Mock(type)

        expect:
        !rule.isInferableInitializer(initializer)

        where:
        type << [ASTNullLiteral, ASTArrayInitializer, ASTLambdaExpression, ASTMethodReference]
    }

    def 'isInferableInitializer is true for any other initializer'() {
        UseVarForLocalVariables rule = new UseVarForLocalVariables()
        ASTExpression initializer = Mock()

        expect:
        rule.isInferableInitializer(initializer)
    }
}
