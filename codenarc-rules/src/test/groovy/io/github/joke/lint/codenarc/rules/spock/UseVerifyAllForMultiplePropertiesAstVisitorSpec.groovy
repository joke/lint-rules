package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.syntax.Token
import org.codenarc.rule.junit.SpockUtil
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each method of {@link UseVerifyAllForMultiplePropertiesRule.UseVerifyAllForMultiplePropertiesAstVisitor} is
 * tested directly. The visitor is spied where its siblings are stubbed. Where a decision reads the closure's
 * own statements, the closure is built as nodes and {@code SpockUtil}'s boolean test is stubbed with
 * {@code SpyStatic}.
 */
@Tag('unit')
class UseVerifyAllForMultiplePropertiesAstVisitorSpec extends Specification {

    UseVerifyAllForMultiplePropertiesRule.UseVerifyAllForMultiplePropertiesAstVisitor visitor = Spy()

    def 'visitBlock reports the constraint closures of every block, whatever its label'() {
        Statement first = statement(constant(1))
        Statement second = statement(constant(2))

        when:
        visitor.visitBlock('when', [first, second])

        then:
        1 * visitor.reportConstraintClosures(first) >> {}
        1 * visitor.reportConstraintClosures(second) >> {}
        1 * visitor._
        0 * _
    }

    def 'reportConstraintClosures reports each argument of the statement'() {
        Statement statement = statement(constant(1))
        Expression first = closure(constant(1))
        Expression second = constant(2)

        when:
        visitor.reportConstraintClosures(statement)

        then:
        1 * visitor.argumentsOf(statement) >> [first, second]
        1 * visitor.reportClosureArgument(first) >> {}
        1 * visitor.reportClosureArgument(second) >> {}
        1 * visitor._
        0 * _
    }

    def 'argumentsOf is the arguments of an interaction statement'() {
        def closure = closure(constant(1))

        expect:
        visitor.argumentsOf(new ExpressionStatement(interaction(closure)))*.is(closure) == [true]
    }

    def 'argumentsOf is empty for a statement that is not an expression, or is not an interaction'() {
        expect:
        visitor.argumentsOf(new EmptyStatement()).empty
        visitor.argumentsOf(new ExpressionStatement(call('save', closure(constant(1))))).empty
    }

    def 'reportClosureArgument adds a violation for a closure that asserts several properties'() {
        Expression argument = closure(constant(1))

        when:
        visitor.reportClosureArgument(argument)

        then:
        1 * visitor.assertsSeveralProperties(argument) >> true
        1 * visitor.addViolation(argument,
                'Assert the properties inside \'verifyAll\', so a value wrong in two fields reports both failures rather than the first.') >> {}
        1 * visitor._
        0 * _
    }

    def 'reportClosureArgument adds no violation for a closure that asserts one property'() {
        Expression argument = closure(constant(1))

        when:
        visitor.reportClosureArgument(argument)

        then:
        1 * visitor.assertsSeveralProperties(argument) >> false
        1 * visitor._
        0 * _
    }

    def 'reportClosureArgument ignores an argument that is not a closure'() {
        when:
        visitor.reportClosureArgument(constant(2))

        then:
        1 * visitor._
        0 * _
    }

    def 'assertsSeveralProperties is true for two conditions in the closure'() {
        SpyStatic(SpockUtil)
        def real = new UseVerifyAllForMultiplePropertiesRule.UseVerifyAllForMultiplePropertiesAstVisitor()
        Statement first = statement(binary('==', var('total'), var('amount')))
        Statement second = statement(binary('==', var('currency'), var('code')))
        ClosureExpression closure = closureOf(first, second)

        when:
        def result = real.assertsSeveralProperties(closure)

        then:
        1 * SpockUtil.isBooleanExpression(first) >> true
        1 * SpockUtil.isBooleanExpression(second) >> true
        0 * _

        expect:
        result
    }

    def 'assertsSeveralProperties is true for a single conjunction'() {
        SpyStatic(SpockUtil)
        def real = new UseVerifyAllForMultiplePropertiesRule.UseVerifyAllForMultiplePropertiesAstVisitor()
        Statement only = statement(binary('&&', var('a'), var('b')))
        ClosureExpression closure = closureOf(only)

        when:
        def result = real.assertsSeveralProperties(closure)

        then:
        1 * SpockUtil.isBooleanExpression(only) >> true
        0 * _

        expect:
        result
    }

    def 'assertsSeveralProperties is false for a single condition that is not a conjunction'() {
        SpyStatic(SpockUtil)
        def real = new UseVerifyAllForMultiplePropertiesRule.UseVerifyAllForMultiplePropertiesAstVisitor()
        Statement only = statement(binary('==', var('total'), var('amount')))
        ClosureExpression closure = closureOf(only)

        when:
        def result = real.assertsSeveralProperties(closure)

        then:
        1 * SpockUtil.isBooleanExpression(only) >> true
        0 * _

        expect:
        !result
    }

    def 'conditionsIn is the expressions of the boolean statements of the closure'() {
        SpyStatic(SpockUtil)
        def real = new UseVerifyAllForMultiplePropertiesRule.UseVerifyAllForMultiplePropertiesAstVisitor()
        Statement first = statement(binary('==', var('total'), var('amount')))
        Statement notACondition = statement(constant(1))
        Statement second = statement(binary('==', var('currency'), var('code')))
        ClosureExpression closure = closureOf(first, notACondition, second)

        when:
        def conditions = real.conditionsIn(closure)

        then:
        1 * SpockUtil.isBooleanExpression(first) >> true
        1 * SpockUtil.isBooleanExpression(notACondition) >> false
        1 * SpockUtil.isBooleanExpression(second) >> true
        0 * _

        expect:
        conditions.size() == 2
        conditions[0].is(first.expression)
        conditions[1].is(second.expression)
    }

    def 'isCondition is decided by SpockUtil for an expression statement'() {
        SpyStatic(SpockUtil)
        def real = new UseVerifyAllForMultiplePropertiesRule.UseVerifyAllForMultiplePropertiesAstVisitor()
        Statement statement = statement(constant(1))

        when:
        def result = real.isCondition(statement)

        then:
        1 * SpockUtil.isBooleanExpression(statement) >> booleanExpression
        0 * _

        expect:
        result == booleanExpression

        where:
        booleanExpression << [true, false]
    }

    def 'isCondition is false for a statement that is not an expression'() {
        expect:
        !visitor.isCondition(new EmptyStatement())
    }

    def 'isConjunction is true only for the logical and'() {
        expect:
        visitor.isConjunction(binary(operator, var('a'), var('b'))) == expected
        !visitor.isConjunction(var('a'))

        where:
        operator | expected
        '&&'     | true
        '||'     | false
        '=='     | false
    }

    private static BinaryExpression binary(String operator, Expression left, Expression right) {
        new BinaryExpression(left, Token.newSymbol(operator, 1, 1), right)
    }

    private static VariableExpression var(String name) {
        new VariableExpression(name)
    }

    private static ConstantExpression constant(Object value) {
        new ConstantExpression(value)
    }

    private static MethodCallExpression call(String name, Expression... arguments) {
        new MethodCallExpression(var('repository'), name, new ArgumentListExpression(arguments))
    }

    private static Expression interaction(Expression argument) {
        binary('*', constant(1), call('save', argument))
    }

    private static ClosureExpression closure(Expression body) {
        closureOf(statement(body))
    }

    private static ClosureExpression closureOf(Statement... statements) {
        new ClosureExpression(null, new BlockStatement(statements.toList(), null))
    }

    private static Statement statement(Expression expression) {
        new ExpressionStatement(expression)
    }
}
