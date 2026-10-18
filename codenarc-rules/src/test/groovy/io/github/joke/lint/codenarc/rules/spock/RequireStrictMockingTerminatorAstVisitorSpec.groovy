package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.syntax.Token
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each method of {@link RequireStrictMockingTerminatorRule.RequireStrictMockingTerminatorAstVisitor} is tested
 * directly. The visitor is spied so the method under test runs for real while the siblings it calls are stubbed.
 */
@Tag('unit')
class RequireStrictMockingTerminatorAstVisitorSpec extends Specification {

    RequireStrictMockingTerminatorRule.RequireStrictMockingTerminatorAstVisitor visitor = Spy()

    def 'visitBlock checks the last statement of a then block'() {
        Statement first = statement()
        Statement last = statement()

        when:
        visitor.visitBlock('then', [first, last])

        then:
        1 * visitor.reportMissingTerminator(last) >> {}
        1 * visitor._
        0 * _
    }

    def 'visitBlock checks nothing outside a then block'() {
        when:
        visitor.visitBlock('when', [statement()])

        then:
        1 * visitor._
        0 * _
    }

    def 'reportMissingTerminator adds a violation when the statement is not the terminator'() {
        Statement statement = statement()

        when:
        visitor.reportMissingTerminator(statement)

        then:
        1 * visitor.isTerminator(statement) >> false
        1 * visitor.addViolation(statement,
                "End 'then:' with '0 * _', so an interaction nobody declared fails the specification.") >> {}
        1 * visitor._
        0 * _
    }

    def 'reportMissingTerminator adds no violation for the terminator'() {
        Statement statement = statement()

        when:
        visitor.reportMissingTerminator(statement)

        then:
        1 * visitor.isTerminator(statement) >> true
        1 * visitor._
        0 * _
    }

    def 'isTerminator is true for the strict mocking terminator, and false for an ordinary interaction'() {
        expect:
        visitor.isTerminator(new ExpressionStatement(
                new BinaryExpression(new ConstantExpression(0), Token.newSymbol('*', 1, 1), new VariableExpression('_'))))
        !visitor.isTerminator(new ExpressionStatement(
                new BinaryExpression(new ConstantExpression(1), Token.newSymbol('*', 1, 1), call())))
    }

    def 'isTerminator is false for a statement that is not an expression'() {
        expect:
        !visitor.isTerminator(new EmptyStatement())
    }

    private static Statement statement() {
        new ExpressionStatement(new ConstantExpression('x'))
    }

    private static MethodCallExpression call() {
        new MethodCallExpression(new VariableExpression('repository'), 'save', new ArgumentListExpression())
    }
}
