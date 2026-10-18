package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MapEntryExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import spock.lang.Specification
import spock.lang.Tag

/**
 * {@link MockCall} reads the arguments of one factory call. Each call is built as nodes, with named
 * arguments collected into a single map placed first, as the Groovy parser leaves them.
 */
@Tag('unit')
class MockCallSpec extends Specification {

    def 'isSpy is true only for a call to Spy'() {
        expect:
        new MockCall(call(name, [])).spy == expected

        where:
        name    | expected
        'Spy'   | true
        'Stub'  | false
    }

    def 'isUnverifiedStub is true for a Stub that does not pass verified'() {
        expect:
        new MockCall(call(name, args)).unverifiedStub == expected

        where:
        name   | args                                | expected
        'Stub' | []                                  | true
        'Stub' | [named('name', 'users')]            | true
        'Stub' | [named('verified', true)]           | false
        'Mock' | []                                  | false
    }

    def 'hasNamedArgument finds a key among the named arguments'() {
        expect:
        new MockCall(call('Stub', [named('name', 'users')])).hasNamedArgument(key) == expected

        where:
        key    | expected
        'name' | true
        'other' | false
    }

    def 'hasNamedArgument is false for a call with no named arguments'() {
        expect:
        !new MockCall(call('Stub', [new VariableExpression('Repository')])).hasNamedArgument('name')
    }

    def 'typeArgumentName is empty for a call with no type argument'() {
        expect:
        !new MockCall(call('Mock', [])).typeArgumentName().present
    }

    def 'typeArgumentName is empty when the only argument is named'() {
        expect:
        !new MockCall(call('Spy', [named('constructorArgs', 'x')])).typeArgumentName().present
    }

    def 'typeArgumentName is the variable named as the type argument, skipping named arguments first'() {
        expect:
        new MockCall(call('Spy', [named('name', 'x'), new VariableExpression('Repository')]))
                .typeArgumentName() == Optional.of('Repository')
    }

    def 'nameOf reads a variable'() {
        expect:
        new MockCall(call('Mock', [])).nameOf(new VariableExpression('Repository')) == Optional.of('Repository')
    }

    def 'nameOf reads a property chain'() {
        expect:
        new MockCall(call('Mock', [])).nameOf(qualified('java', 'util', 'List')) == Optional.of('java.util.List')
    }

    def 'nameOf is empty for an expression that names no type'() {
        expect:
        !new MockCall(call('Mock', [])).nameOf(new ConstantExpression('Repository')).present
    }

    def 'nameOfProperty drops a trailing class property'() {
        expect:
        new MockCall(call('Mock', [])).nameOfProperty(new PropertyExpression(
                new VariableExpression('Repository'), 'class')) == Optional.of('Repository')
    }

    def 'nameOfProperty joins a property to the name of its owner'() {
        expect:
        new MockCall(call('Mock', [])).nameOfProperty(new PropertyExpression(
                new VariableExpression('Outer'), 'Inner')) == Optional.of('Outer.Inner')
    }

    def 'nameOfProperty is empty when its owner names no type'() {
        expect:
        !new MockCall(call('Mock', [])).nameOfProperty(new PropertyExpression(
                new ConstantExpression('x'), 'class')).present
    }

    def 'repeats is true for the declared type named as the type argument'() {
        expect:
        new MockCall(call('Mock', [new VariableExpression('Repository')])).repeats(new ClassNode('Repository', 0, null))
    }

    def 'repeats is false for another type, and for a call that names none'() {
        expect:
        !new MockCall(call('Mock', [new VariableExpression('Other')])).repeats(new ClassNode('Repository', 0, null))
        !new MockCall(call('Mock', [])).repeats(new ClassNode('Repository', 0, null))
    }

    def 'isSameType matches a simple name with a qualified one, in either order'() {
        expect:
        new MockCall(call('Mock', [])).isSameType(argument, declared) == expected

        where:
        argument               | declared               | expected
        'Foo'                  | 'Foo'                  | true
        'Foo'                  | 'com.example.Foo'      | true
        'com.example.Foo'      | 'Foo'                  | true
        'java.util.List'       | 'java.awt.List'        | false
        'List'                 | 'Set'                  | false
        'oo'                   | 'Foo'                  | false
    }

    private static MethodCallExpression call(String name, List<Expression> arguments) {
        def list = new ArgumentListExpression()
        arguments.each { list.addExpression(it) }
        new MethodCallExpression(new VariableExpression('this'), new ConstantExpression(name), list)
    }

    private static MapExpression named(String key, Object value) {
        def entry = new MapEntryExpression(new ConstantExpression(key), new ConstantExpression(value))
        new MapExpression([entry])
    }

    private static PropertyExpression qualified(String... segments) {
        Expression expression = new VariableExpression(segments[0])
        segments.tail().each { expression = new PropertyExpression(expression, it) }
        expression
    }
}
