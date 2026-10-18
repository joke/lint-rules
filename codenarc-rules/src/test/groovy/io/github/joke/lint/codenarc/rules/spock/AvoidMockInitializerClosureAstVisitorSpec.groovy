package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codenarc.rule.junit.SpockUtil
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Each method of {@link AvoidMockInitializerClosureRule.AvoidMockInitializerClosureAstVisitor} is tested directly.
 * The visitor is spied, and the closure argument is read through {@code SpockUtil}, which is stubbed with
 * {@code SpyStatic}.
 */
@Tag('unit')
@Subject(AvoidMockInitializerClosureRule.AvoidMockInitializerClosureAstVisitor)
class AvoidMockInitializerClosureAstVisitorSpec extends Specification {

    AvoidMockInitializerClosureRule.AvoidMockInitializerClosureAstVisitor visitor = Spy()
    MethodCallExpression call = call('Mock')

    def 'visitMethodCallExpression reports the call, then visits its parts'() {
        when:
        visitor.visitMethodCallExpression(call)

        then:
        1 * visitor.reportInitializerClosure(call) >> {}
        1 * visitor.visitVariableExpression(call.objectExpression) >> {}
        1 * visitor.visitConstantExpression(call.method) >> {}
        1 * visitor.visitArgumentlistExpression(call.arguments) >> {}
        1 * visitor._
        0 * _
    }

    def 'reportInitializerClosure adds a violation for a first-visited mock factory call with a closure'() {
        when:
        visitor.reportInitializerClosure(call)

        then:
        1 * visitor.isFirstVisit(call) >> firstVisit
        (firstVisit ? 1 : 0) * visitor.isMockFactory(call) >> factory
        (firstVisit && factory ? 1 : 0) * visitor.hasClosureArgument(call) >> closure
        (firstVisit && factory && closure ? 1 : 0) * visitor.addViolation(call,
                "Declare the interaction in 'then:' with its cardinality and its real argument, not in the mock's initialiser.") >> {}
        1 * visitor._
        0 * _

        where:
        firstVisit | factory | closure | _
        true       | true    | true    | _
        false      | true    | true    | _
        true       | false   | true    | _
        true       | true    | false   | _
    }

    def 'isMockFactory accepts Mock, Stub, Spy and DeepMock, and nothing else'() {
        expect:
        visitor.isMockFactory(call(name)) == expected

        where:
        name        | expected
        'Mock'      | true
        'Stub'      | true
        'Spy'       | true
        'DeepMock'  | true
        'SpyStatic' | false
    }

    def 'isMockFactory is false for a dynamically named call'() {
        expect:
        !visitor.isMockFactory(new MethodCallExpression(
                new VariableExpression('this'), new VariableExpression('name'), new ArgumentListExpression()))
    }

    def 'hasClosureArgument is decided by the closure argument of the call'() {
        SpyStatic(SpockUtil)
        def real = new AvoidMockInitializerClosureRule.AvoidMockInitializerClosureAstVisitor()

        when:
        def result = real.hasClosureArgument(call)

        then:
        1 * SpockUtil.getClosureArgument(call) >> closure
        0 * _

        expect:
        result == expected

        where:
        closure                                                | expected
        new ClosureExpression(null, new BlockStatement())      | true
        null                                                   | false
    }

    private static MethodCallExpression call(String name) {
        new MethodCallExpression(new VariableExpression('this'), new ConstantExpression(name), new ArgumentListExpression())
    }
}
