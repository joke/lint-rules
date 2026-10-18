package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * {@link DoubleScope} collects the doubles of one kind in scope for a feature method, from the
 * specification's fields and from the locals of its blocks. A double is named by its declaration, so every
 * declaration is built as nodes.
 */
@Tag('unit')
@Subject(DoubleScope)
class DoubleScopeSpec extends Specification {

    def 'enter collects the fields initialised by a factory of the kind'() {
        def classNode = new ClassNode('ExampleSpec', 0, null)
        classNode.addField(new FieldNode('repository', 0, null, null, call('Spy', [])))
        classNode.addField(new FieldNode('clock', 0, null, null, call('Mock', [])))
        def scope = new DoubleScope({ MockCall call -> call.spy })

        when:
        scope.enter(classNode)

        then:
        0 * _

        expect:
        scope.doubleNamedBy(new VariableExpression('repository')) == Optional.of('repository')
        !scope.doubleNamedBy(new VariableExpression('clock')).present
    }

    def 'enter forgets the locals of the previous feature method'() {
        def scope = new DoubleScope({ MockCall call -> call.spy })
        scope.collectDeclaration(declaration('spy', call('Spy', [])))

        when:
        scope.enter(new ClassNode('ExampleSpec', 0, null))

        then:
        0 * _

        expect:
        !scope.doubleNamedBy(new VariableExpression('spy')).present
    }

    def 'collectLocal records a local initialised by a factory of the kind'() {
        def scope = new DoubleScope({ MockCall call -> call.spy })

        when:
        scope.collectLocal(new ExpressionStatement(declaration('spy', call('Spy', []))))

        then:
        0 * _

        expect:
        scope.doubleNamedBy(new VariableExpression('spy')) == Optional.of('spy')
    }

    def 'collectLocal ignores a statement that is not an expression'() {
        def scope = new DoubleScope({ MockCall call -> call.spy })

        when:
        scope.collectLocal(new org.codehaus.groovy.ast.stmt.EmptyStatement())

        then:
        0 * _

        expect:
        !scope.doubleNamedBy(new VariableExpression('spy')).present
    }

    def 'collectDeclaration ignores an expression that is not a declaration'() {
        def scope = new DoubleScope({ MockCall call -> call.spy })

        when:
        scope.collectDeclaration(new VariableExpression('spy'))

        then:
        0 * _

        expect:
        !scope.doubleNamedBy(new VariableExpression('spy')).present
    }

    def 'collectDeclaration ignores a declaration initialised by something other than a factory of the kind'() {
        def scope = new DoubleScope({ MockCall call -> call.spy })

        when:
        scope.collectDeclaration(declaration('stub', call('Stub', [])))
        scope.collectDeclaration(declaration('value', new ConstantExpression(1)))

        then:
        0 * _

        expect:
        !scope.doubleNamedBy(new VariableExpression('stub')).present
        !scope.doubleNamedBy(new VariableExpression('value')).present
    }

    def 'collectDeclaration ignores a declaration whose left side is not a plain variable'() {
        def scope = new DoubleScope({ MockCall call -> call.spy })
        def multiple = new DeclarationExpression(
                new TupleExpression(new VariableExpression('a'), new VariableExpression('b')),
                Token.newSymbol(Types.ASSIGN, 1, 1),
                call('Spy', []))

        when:
        scope.collectDeclaration(multiple)

        then:
        0 * _

        expect:
        !scope.doubleNamedBy(new VariableExpression('a')).present
    }

    def 'doubleNamedBy finds the receiver of a method call or a property access'() {
        def scope = new DoubleScope({ MockCall call -> call.spy })
        scope.collectDeclaration(declaration('spy', call('Spy', [])))

        expect:
        scope.doubleNamedBy(target) == expected

        where:
        target                                                                         | expected
        new MethodCallExpression(new VariableExpression('spy'), 'run', new ArgumentListExpression()) | Optional.of('spy')
        new PropertyExpression(new VariableExpression('spy'), 'name')                  | Optional.of('spy')
        new VariableExpression('spy')                                                  | Optional.of('spy')
        new VariableExpression('other')                                                | Optional.empty()
        new ConstantExpression('spy')                                                  | Optional.empty()
    }

    def 'nameOf is the name of a variable and empty for anything else'() {
        def scope = new DoubleScope({ MockCall call -> call.spy })

        expect:
        scope.nameOf(new VariableExpression('spy')) == Optional.of('spy')
        !scope.nameOf(new ConstantExpression('spy')).present
    }

    private static DeclarationExpression declaration(String name, Expression initialiser) {
        new DeclarationExpression(new VariableExpression(name), Token.newSymbol(Types.ASSIGN, 1, 1), initialiser)
    }

    private static MethodCallExpression call(String name, List<Expression> arguments) {
        def list = new ArgumentListExpression()
        arguments.each { list.addExpression(it) }
        new MethodCallExpression(new VariableExpression('this'), new ConstantExpression(name), list)
    }
}
