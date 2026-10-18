package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * {@link MockFactories} reads a call by its name. The calls here are built as nodes, so the names are the
 * only thing under test.
 */
@Tag('unit')
@Subject(MockFactories)
class MockFactoriesSpec extends Specification {

    MockFactories factories = new MockFactories()

    def 'read is empty for a missing expression'() {
        expect:
        factories.read(null).empty
    }

    def 'read is empty for an expression that is not a method call'() {
        expect:
        factories.read(new VariableExpression('mock')).empty
    }

    def 'read is present for a call to a factory'() {
        expect:
        factories.read(call('Mock')).present
    }

    def 'read is empty for a call that is not a factory'() {
        expect:
        factories.read(call('SpyStatic')).empty
    }

    def 'isFactory accepts Mock, Stub, Spy and DeepMock, and nothing else'() {
        expect:
        factories.isFactory(call(name)) == expected

        where:
        name           | expected
        'Mock'         | true
        'Stub'         | true
        'Spy'          | true
        'DeepMock'     | true
        'SpyStatic'    | false
        'MockRepository' | false
        'GroovyMock'   | false
    }

    def 'isFactory is false for a dynamically named call, which has no method name'() {
        expect:
        !factories.isFactory(new MethodCallExpression(
                new VariableExpression('this'), new VariableExpression('name'), new ArgumentListExpression()))
    }

    private static MethodCallExpression call(String name) {
        new MethodCallExpression(
                new VariableExpression('this'), new ConstantExpression(name), new ArgumentListExpression())
    }
}
