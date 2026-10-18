package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.RangeExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.syntax.Token
import spock.lang.Specification
import spock.lang.Tag

/**
 * {@link SpockInteraction} reads one statement as the interaction it may be. Each statement is built as
 * nodes in the shape the Groovy parser gives it, so a form is recognised or rejected on its structure.
 */
@Tag('unit')
class SpockInteractionSpec extends Specification {

    def 'a counted interaction is required unless its count is the wildcard'() {
        expect:
        def interaction = new SpockInteraction(statement)
        interaction.counted == counted
        interaction.required == required

        where:
        statement                          | counted | required
        bin('*', count(1), foo())          | true    | true
        bin('*', count(0), foo())          | true    | true
        bin('*', range(), foo())           | true    | true
        bin('*', wildcard(), foo())        | true    | false
    }

    def 'a stubbed interaction is required when its count is, but it is not counted'() {
        expect:
        def interaction = new SpockInteraction(statement)
        interaction.counted == false
        interaction.required == required

        where:
        statement                                              | required
        bin('>>', bin('*', count(1), foo()), constant('x'))    | true
        bin('>>', bin('*', wildcard(), foo()), constant('x'))  | false
        bin('>>>', bin('*', count(2), foo()), list())          | true
    }

    def 'a stub with no count is an interaction but is neither counted nor required'() {
        def target = foo()

        when:
        def interaction = new SpockInteraction(bin('>>', target, constant('x')))

        then:
        0 * _

        expect:
        interaction.present
        interaction.target.get().is(target)
        !interaction.counted
        !interaction.required
    }

    def 'the strict mocking terminator is zero times the wildcard'() {
        when:
        def interaction = new SpockInteraction(bin('*', count(0), wildcard()))

        then:
        0 * _

        expect:
        interaction.terminator
        interaction.neverCalled
    }

    def 'a zero count on a named target is never called but is not the terminator'() {
        when:
        def interaction = new SpockInteraction(bin('*', count(0), foo()))

        then:
        0 * _

        expect:
        interaction.neverCalled
        !interaction.terminator
    }

    def 'an entry call is a property access on the wildcard, and nothing else'() {
        expect:
        new SpockInteraction(bin('*', count(1), new PropertyExpression(var('spy'), '_'))).entryCall
        !new SpockInteraction(bin('*', count(1), new PropertyExpression(var('spy'), 'name'))).entryCall
        !new SpockInteraction(bin('*', count(1), foo())).entryCall
    }

    def 'an interaction block is an interaction whose target is the call, and is not counted'() {
        when:
        def interaction = new SpockInteraction(interactionBlock())

        then:
        0 * _

        expect:
        interaction.present
        interaction.target.present
        !interaction.counted
    }

    def 'a plain call with no count, stub or block is not an interaction'() {
        expect:
        !new SpockInteraction(foo()).present
        !new SpockInteraction(foo()).target.present
    }

    def 'a target that is not a call, a property or a variable is not an interaction'() {
        expect:
        !new SpockInteraction(bin('*', count(1), bin('+', var('a'), var('b')))).present
        !new SpockInteraction(bin('>>', bin('+', var('a'), var('b')), constant('x'))).present
    }

    def 'the target of a counted interaction is the call, the property or the variable it names'() {
        expect:
        new SpockInteraction(bin('*', count(1), target)).target.get().is(target)

        where:
        target << [foo(), new PropertyExpression(var('mock'), 'name'), var('mock')]
    }

    def 'the arguments of a call target are its argument expressions, and a property target has none'() {
        expect:
        new SpockInteraction(bin('*', count(1), call('find', constant(1), constant(2)))).arguments.size() == 2
        new SpockInteraction(bin('*', count(1), new PropertyExpression(var('mock'), 'name'))).arguments.empty
    }

    def 'getTarget is empty for an expression that is not an interaction'() {
        expect:
        !new SpockInteraction(constant(1)).target.present
    }

    def 'isInteger is true only for an integer constant'() {
        expect:
        new SpockInteraction(foo()).isInteger(constant(1))
        !new SpockInteraction(foo()).isInteger(constant('1'))
        !new SpockInteraction(foo()).isInteger(var('one'))
        !new SpockInteraction(foo()).isInteger(null)
    }

    def 'isNever is true only for the constant zero'() {
        expect:
        new SpockInteraction(foo()).isNever(constant(0))
        !new SpockInteraction(foo()).isNever(constant(1))
        !new SpockInteraction(foo()).isNever(constant('0'))
        !new SpockInteraction(foo()).isNever(null)
    }

    def 'isWildcard is true only for the variable named underscore'() {
        expect:
        new SpockInteraction(foo()).isWildcard(wildcard())
        !new SpockInteraction(foo()).isWildcard(var('wild'))
        !new SpockInteraction(foo()).isWildcard(null)
    }

    def 'isCount accepts a range, the wildcard and an integer'() {
        expect:
        new SpockInteraction(foo()).isCount(range())
        new SpockInteraction(foo()).isCount(wildcard())
        new SpockInteraction(foo()).isCount(count(1))
        !new SpockInteraction(foo()).isCount(constant('one'))
    }

    def 'isStubbedReturn is true for a response operator and for nothing else'() {
        expect:
        new SpockInteraction(foo()).isStubbedReturn(bin('>>', foo(), constant('x')))
        new SpockInteraction(foo()).isStubbedReturn(bin('>>>', foo(), list()))
        !new SpockInteraction(foo()).isStubbedReturn(bin('*', count(1), foo()))
        !new SpockInteraction(foo()).isStubbedReturn(foo())
    }

    def 'isCardinality is true for a multiplication whose left side is a count'() {
        expect:
        new SpockInteraction(foo()).isCardinality(bin('*', count(1), foo()))
        !new SpockInteraction(foo()).isCardinality(bin('*', var('count'), foo()))
        !new SpockInteraction(foo()).isCardinality(bin('+', count(1), foo()))
        !new SpockInteraction(foo()).isCardinality(foo())
    }

    def 'isInteractionCall is true for a call named interaction with a closure'() {
        expect:
        new SpockInteraction(foo()).isInteractionCall(interactionBlock())
        !new SpockInteraction(foo()).isInteractionCall(foo())
        !new SpockInteraction(foo()).isInteractionCall(call('interaction', constant(1)))
        !new SpockInteraction(foo()).isInteractionCall(closureCall('other'))
    }

    def 'stubbedTargetOf reads the target under a cardinality, and is the expression itself without one'() {
        expect:
        new SpockInteraction(foo()).stubbedTargetOf(bin('*', count(1), target)).is(target)
        new SpockInteraction(foo()).stubbedTargetOf(target).is(target)

        where:
        target << [foo(), new PropertyExpression(var('mock'), 'name')]
    }

    def 'stubbedTargetOf is empty for an expression that is not a target, with or without a cardinality'() {
        expect:
        new SpockInteraction(foo()).stubbedTargetOf(constant(1)) == null
        new SpockInteraction(foo()).stubbedTargetOf(bin('*', count(1), constant(1))) == null
    }

    private static BinaryExpression bin(String op, Expression left, Expression right) {
        new BinaryExpression(left, Token.newSymbol(op, 1, 1), right)
    }

    private static VariableExpression var(String name) {
        new VariableExpression(name)
    }

    private static VariableExpression wildcard() {
        var('_')
    }

    private static ConstantExpression count(int n) {
        new ConstantExpression(n)
    }

    private static ConstantExpression constant(Object value) {
        new ConstantExpression(value)
    }

    private static RangeExpression range() {
        new RangeExpression(count(0), count(2), true)
    }

    private static Expression list() {
        new org.codehaus.groovy.ast.expr.ListExpression([constant('a'), constant('b')])
    }

    private static MethodCallExpression call(String name, Expression... args) {
        new MethodCallExpression(var('this'), name, new ArgumentListExpression(args))
    }

    private static MethodCallExpression foo() {
        call('foo')
    }

    private static MethodCallExpression interactionBlock() {
        closureCall('interaction')
    }

    private static MethodCallExpression closureCall(String name) {
        def closure = new ClosureExpression(null, new BlockStatement())
        new MethodCallExpression(var('this'), name, closure)
    }
}
