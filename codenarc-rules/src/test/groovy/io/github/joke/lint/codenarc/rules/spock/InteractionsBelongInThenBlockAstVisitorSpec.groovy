package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.syntax.Token
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Each method of {@link InteractionsBelongInThenBlockRule.InteractionsBelongInThenBlockAstVisitor} is tested
 * directly. The visitor is spied so the method under test runs for real while the siblings it calls are stubbed.
 */
@Tag('unit')
@Subject(InteractionsBelongInThenBlockRule.InteractionsBelongInThenBlockAstVisitor)
class InteractionsBelongInThenBlockAstVisitorSpec extends Specification {

    InteractionsBelongInThenBlockRule.InteractionsBelongInThenBlockAstVisitor visitor = Spy()

    def 'visitBlock reports each statement of a block that is not a then block'() {
        Statement first = statement()
        Statement second = statement()

        when:
        visitor.visitBlock('when', [first, second])

        then:
        1 * visitor.reportInteraction(first) >> {}
        1 * visitor.reportInteraction(second) >> {}
        1 * visitor._
        0 * _
    }

    def 'visitBlock reports nothing in a then block'() {
        when:
        visitor.visitBlock('then', [statement()])

        then:
        1 * visitor._
        0 * _
    }

    def 'reportInteraction adds a violation for an interaction'() {
        Statement statement = statement()

        when:
        visitor.reportInteraction(statement)

        then:
        1 * visitor.isInteraction(statement) >> true
        1 * visitor.addViolation(statement,
                "Declare the interaction in 'then:', where it verifies the 'when:' that caused it.") >> {}
        1 * visitor._
        0 * _
    }

    def 'reportInteraction adds no violation otherwise'() {
        Statement statement = statement()

        when:
        visitor.reportInteraction(statement)

        then:
        1 * visitor.isInteraction(statement) >> false
        1 * visitor._
        0 * _
    }

    def 'isInteraction is true for an expression statement that is an interaction'() {
        expect:
        visitor.isInteraction(new ExpressionStatement(
                new BinaryExpression(new ConstantExpression(1), Token.newSymbol('*', 1, 1), call('save'))))
    }

    def 'isInteraction is false for a plain call and for a statement that is not an expression'() {
        expect:
        !visitor.isInteraction(new ExpressionStatement(call('save')))
        !visitor.isInteraction(new org.codehaus.groovy.ast.stmt.EmptyStatement())
    }

    private static ExpressionStatement statement() {
        new ExpressionStatement(new ConstantExpression('x'))
    }

    private static MethodCallExpression call(String name) {
        new MethodCallExpression(new VariableExpression('repository'), name, new ArgumentListExpression())
    }
}
