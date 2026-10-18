package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codenarc.rule.junit.SpockUtil
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each method of {@link AvoidSetupAndGivenLabelsRule.AvoidSetupAndGivenLabelsAstVisitor} is tested directly.
 * The visitor is spied so the method under test runs for real while the siblings it calls are stubbed.
 */
@Tag('unit')
class AvoidSetupAndGivenLabelsAstVisitorSpec extends Specification {

    AvoidSetupAndGivenLabelsRule.AvoidSetupAndGivenLabelsAstVisitor visitor = Spy()

    def 'visitMethodEx reports the labels of a feature method'() {
        SpyStatic(SpockUtil)
        MethodNode method = method()

        when:
        visitor.visitMethodEx(method)

        then:
        1 * SpockUtil.isSpockFeatureMethod(method) >> true
        1 * visitor.reportLabelsIn(method) >> {}
        1 * visitor._
        0 * _
    }

    def 'visitMethodEx leaves a fixture method alone'() {
        SpyStatic(SpockUtil)
        MethodNode method = method()

        when:
        visitor.visitMethodEx(method)

        then:
        1 * SpockUtil.isSpockFeatureMethod(method) >> false
        1 * visitor._
        0 * _
    }

    def 'reportLabelsIn reports the statements of the method, in order'() {
        Statement first = statement('setup')
        Statement second = statement('given')
        MethodNode method = method(first, second)

        when:
        visitor.reportLabelsIn(method)

        then:
        1 * visitor.reportLabelsOn(first) >> {}
        1 * visitor.reportLabelsOn(second) >> {}
        1 * visitor._
        0 * _
    }

    def 'reportLabelsOn reports a setup or given label, once each'() {
        Statement statement = statement('setup', 'when', 'setup')

        when:
        visitor.reportLabelsOn(statement)

        then:
        1 * visitor.labelsOf(statement) >> ['setup', 'when', 'setup']
        1 * visitor.report(statement, 'setup') >> {}
        1 * visitor._
        0 * _
    }

    def 'reportLabelsOn reports every reported label a statement carries'() {
        Statement statement = statement('given', 'setup')

        when:
        visitor.reportLabelsOn(statement)

        then:
        1 * visitor.labelsOf(statement) >> ['given', 'setup']
        1 * visitor.report(statement, 'given') >> {}
        1 * visitor.report(statement, 'setup') >> {}
        1 * visitor._
        0 * _
    }

    def 'labelsOf is the statement labels, or nothing for a statement with none'() {
        expect:
        visitor.labelsOf(statement('setup', 'when')) == ['setup', 'when']
        visitor.labelsOf(new ExpressionStatement(new ConstantExpression('x'))) == []
    }

    def 'report adds a violation naming the label'() {
        Statement statement = statement('setup')

        when:
        visitor.report(statement, 'setup')

        then:
        1 * visitor.addViolation(statement,
                "Spock treats unlabelled statements at the top of a feature method as the setup block, so 'setup:' adds nothing.") >> {}
        1 * visitor._
        0 * _
    }

    private static Statement statement(String... labels) {
        def statement = new ExpressionStatement(new ConstantExpression('x'))
        labels.each { statement.addStatementLabel(it) }
        statement
    }

    private static MethodNode method(Statement... statements) {
        new MethodNode('feature', 0, ClassHelper.VOID_TYPE, [] as Parameter[], [] as org.codehaus.groovy.ast.ClassNode[],
                new BlockStatement(statements.toList(), null))
    }
}
