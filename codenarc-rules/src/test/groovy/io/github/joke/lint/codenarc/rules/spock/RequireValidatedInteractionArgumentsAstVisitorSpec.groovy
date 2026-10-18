package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.SpreadExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.syntax.Token
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each method of {@link RequireValidatedInteractionArgumentsRule.RequireValidatedInteractionArgumentsAstVisitor}
 * is tested directly. The visitor is spied so the method under test runs for real while the siblings it calls
 * are stubbed. The pure decisions run on the real visitor over built nodes.
 */
@Tag('unit')
class RequireValidatedInteractionArgumentsAstVisitorSpec extends Specification {

    RequireValidatedInteractionArgumentsRule.RequireValidatedInteractionArgumentsAstVisitor visitor = Spy()

    def 'reportUnconstrainedArguments reports each argument of the interaction it reads'() {
        def argument = new VariableExpression('_')
        def expression = interaction(argument)

        when:
        visitor.reportUnconstrainedArguments(expression)

        then:
        1 * visitor.reportUnconstrained(argument) >> {}
        1 * visitor._
        0 * _
    }

    def 'visitBinaryExpression reads the interaction it is given, then visits its operands'() {
        BinaryExpression expression = Mock()
        Expression left = Mock()
        Expression right = Mock()

        when:
        visitor.visitBinaryExpression(expression)

        then:
        1 * visitor.reportUnconstrainedArguments(expression) >> {}
        1 * expression.leftExpression >> left
        1 * expression.rightExpression >> right
        1 * left.visit(visitor)
        1 * right.visit(visitor)
        1 * visitor._
        0 * _
    }

    def 'reportUnconstrainedArguments reads nothing from an expression that is not an interaction'() {
        when:
        visitor.reportUnconstrainedArguments(new BinaryExpression(
                new VariableExpression('a'), Token.newSymbol('+', 1, 1), new VariableExpression('b')))

        then:
        1 * visitor._
        0 * _
    }

    def 'reportUnconstrained adds a violation for a first-visited argument that matches anything'() {
        def argument = new VariableExpression('_')

        when:
        visitor.reportUnconstrained(argument)

        then:
        1 * visitor.isFirstVisit(argument) >> true
        1 * visitor.matchesAnything(argument) >> true
        1 * visitor.addViolation(argument,
                'Pass the value the interaction expects: an argument that matches anything asserts only that a call happened.') >> {}
        1 * visitor._
        0 * _
    }

    def 'reportUnconstrained reports nothing for an argument already visited, or one that constrains'() {
        def argument = new VariableExpression('_')

        when:
        visitor.reportUnconstrained(argument)

        then:
        1 * visitor.isFirstVisit(argument) >> firstVisit
        (firstVisit ? 1 : 0) * visitor.matchesAnything(argument) >> matches
        0 * visitor.addViolation(argument, 'Pass the value the interaction expects: an argument that matches anything asserts only that a call happened.')
        1 * visitor._
        0 * _

        where:
        firstVisit | matches
        false      | true
        true       | false
    }

    def 'matchesAnything is true for the wildcard, a cast or spread of it, and a closure that is always true'() {
        expect:
        visitor.matchesAnything(argument) == expected

        where:
        argument                                                           | expected
        new VariableExpression('_')                                       | true
        new CastExpression(ClassHelper.make(String), new VariableExpression('_')) | true
        new SpreadExpression(new VariableExpression('_'))                 | true
        closure(constant(1))                                              | true
        closure(constant(0))                                              | false
        constant('x')                                                     | false
    }

    def 'unwrapped is the expression a cast or spread narrows, and the expression itself otherwise'() {
        def wildcard = new VariableExpression('_')

        expect:
        visitor.unwrapped(new CastExpression(ClassHelper.make(String), wildcard)).is(wildcard)
        visitor.unwrapped(new SpreadExpression(wildcard)).is(wildcard)
        visitor.unwrapped(wildcard).is(wildcard)
    }

    def 'isWildcard is true only for the variable named underscore'() {
        expect:
        visitor.isWildcard(expression) == expected

        where:
        expression                     | expected
        new VariableExpression('_')    | true
        new VariableExpression('name') | false
        constant(1)                    | false
    }

    def 'isAlwaysTrue is true for a closure whose constant body is truthy'() {
        expect:
        visitor.isAlwaysTrue(argument) == expected

        where:
        argument                | expected
        closure(constant(1))    | true
        closure(constant(0))    | false
        constant(1)             | false
    }

    def 'constantBodyOf is the value of a closure whose only statement is a constant'() {
        expect:
        visitor.constantBodyOf(closure(constant(1))) == Optional.of(1)
        !visitor.constantBodyOf(closure(new VariableExpression('name'))).present
        !visitor.constantBodyOf(new ClosureExpression(null, new BlockStatement([new ReturnStatement(constant(1))], null))).present
        !visitor.constantBodyOf(new ClosureExpression(null, new BlockStatement([statement(constant(1)), statement(constant(2))], null))).present
    }

    def 'onlyStatementOf is the one statement of a closure, and nothing when it has several'() {
        def only = statement(constant(1))

        expect:
        visitor.onlyStatementOf(new ClosureExpression(null, new BlockStatement([only], null))) == Optional.of(only)
        !visitor.onlyStatementOf(new ClosureExpression(null, new BlockStatement([only, only], null))).present
    }

    private static BinaryExpression interaction(Expression argument) {
        def call = new MethodCallExpression(new VariableExpression('repository'), 'find',
                new ArgumentListExpression([argument]))
        new BinaryExpression(new ConstantExpression(1), Token.newSymbol('*', 1, 1), call)
    }

    private static ConstantExpression constant(Object value) {
        new ConstantExpression(value)
    }

    private static ClosureExpression closure(Expression body) {
        new ClosureExpression(null, new BlockStatement([statement(body)], null))
    }

    private static Statement statement(Expression expression) {
        new ExpressionStatement(expression)
    }
}
