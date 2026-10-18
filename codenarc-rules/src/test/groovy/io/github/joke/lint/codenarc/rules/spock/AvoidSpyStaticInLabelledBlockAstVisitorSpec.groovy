package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Each method of {@link AvoidSpyStaticInLabelledBlockRule.AvoidSpyStaticInLabelledBlockAstVisitor} is tested
 * directly. The visitor is spied so the method under test runs for real while the siblings it calls are stubbed.
 */
@Tag('unit')
@Subject(AvoidSpyStaticInLabelledBlockRule.AvoidSpyStaticInLabelledBlockAstVisitor)
class AvoidSpyStaticInLabelledBlockAstVisitorSpec extends Specification {

    AvoidSpyStaticInLabelledBlockRule.AvoidSpyStaticInLabelledBlockAstVisitor visitor = Spy()

    def 'visitBlock reports the SpyStatic calls of a block that follows a label'() {
        Statement first = statement('given')
        Statement second = statement('when')
        def statements = [first, second]

        when:
        visitor.visitBlock('given', statements)

        then:
        1 * visitor.isLabelled(statements) >> labelled
        (labelled ? 1 : 0) * visitor.reportSpyStatic(first) >> {}
        (labelled ? 1 : 0) * visitor.reportSpyStatic(second) >> {}
        1 * visitor._
        0 * _

        where:
        labelled << [true, false]
    }

    def 'isLabelled is true when the first statement of the block carries a label'() {
        expect:
        visitor.isLabelled(statements) == expected

        where:
        statements                                 | expected
        []                                         | false
        [statement('given')]                       | true
        [statement(), statement('when')]           | false
    }

    def 'reportSpyStatic reports a SpyStatic call, and nothing else'() {
        Statement statement = statement()

        when:
        visitor.reportSpyStatic(statement)

        then:
        1 * visitor.isSpyStaticCall(statement.expression) >> isSpyStatic
        (isSpyStatic ? 1 : 0) * visitor.addViolation(statement,
                "Call 'SpyStatic' with the unlabelled setup statements: it arranges the whole feature method rather than verifying one call.") >> {}
        1 * visitor._
        0 * _

        where:
        isSpyStatic << [true, false]
    }

    def 'reportSpyStatic reports nothing for a statement that is not an expression'() {
        when:
        visitor.reportSpyStatic(new EmptyStatement())

        then:
        1 * visitor._
        0 * _
    }

    def 'isSpyStaticCall is true only for a call whose whole name is SpyStatic'() {
        expect:
        visitor.isSpyStaticCall(call(name)) == expected

        where:
        name            | expected
        'SpyStatic'     | true
        'MySpyStatic'   | false
        'SpyStaticHelp' | false
        'Spy'           | false
    }

    def 'isSpyStaticCall is false for an expression that is not a call'() {
        expect:
        !visitor.isSpyStaticCall(new VariableExpression('SpyStatic'))
        !visitor.isSpyStaticCall(new ConstantExpression('SpyStatic'))
    }

    private static Statement statement(String... labels) {
        def statement = new ExpressionStatement(call('SpyStatic'))
        labels.each { statement.addStatementLabel(it) }
        statement
    }

    private static MethodCallExpression call(String name) {
        new MethodCallExpression(new VariableExpression('this'), name, new ArgumentListExpression())
    }
}
