package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codenarc.rule.junit.SpockUtil
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Each method of {@link ValueAssertionsBelongInExpectBlockRule.ValueAssertionsBelongInExpectBlockAstVisitor} is
 * tested directly. The visitor is spied so the method under test runs for real while the siblings it calls are
 * stubbed; the value-assertion test is {@code SpockUtil}'s, which is stubbed with {@code SpyStatic}.
 */
@Tag('unit')
@Subject(ValueAssertionsBelongInExpectBlockRule.ValueAssertionsBelongInExpectBlockAstVisitor)
class ValueAssertionsBelongInExpectBlockAstVisitorSpec extends Specification {

    ValueAssertionsBelongInExpectBlockRule.ValueAssertionsBelongInExpectBlockAstVisitor visitor = Spy()

    def 'visitBlock reports each statement of a then block'() {
        Statement first = statement()
        Statement second = statement()

        when:
        visitor.visitBlock('then', [first, second])

        then:
        1 * visitor.reportValueAssertion(first) >> {}
        1 * visitor.reportValueAssertion(second) >> {}
        1 * visitor._
        0 * _
    }

    def 'visitBlock reports nothing outside a then block'() {
        when:
        visitor.visitBlock('when', [statement()])

        then:
        1 * visitor._
        0 * _
    }

    def 'reportValueAssertion adds a violation for a value assertion'() {
        Statement statement = statement()

        when:
        visitor.reportValueAssertion(statement)

        then:
        1 * visitor.isValueAssertion(statement) >> true
        1 * visitor.addViolation(statement, "'then:' states who was called; move the value assertion to 'expect:'.") >> {}
        1 * visitor._
        0 * _
    }

    def 'reportValueAssertion adds no violation otherwise'() {
        Statement statement = statement()

        when:
        visitor.reportValueAssertion(statement)

        then:
        1 * visitor.isValueAssertion(statement) >> false
        1 * visitor._
        0 * _
    }

    def 'isValueAssertion is decided by SpockUtil for an expression statement'() {
        SpyStatic(SpockUtil)
        ExpressionStatement statement = statement()

        when:
        def result = visitor.isValueAssertion(statement)

        then:
        1 * SpockUtil.isBooleanExpression(statement) >> booleanExpression
        1 * visitor._
        0 * _

        expect:
        result == booleanExpression

        where:
        booleanExpression << [true, false]
    }

    def 'isValueAssertion is false for a statement that is not an expression'() {
        expect:
        !visitor.isValueAssertion(new org.codehaus.groovy.ast.stmt.EmptyStatement())
    }

    private static ExpressionStatement statement() {
        new ExpressionStatement(new ConstantExpression('x'))
    }
}
