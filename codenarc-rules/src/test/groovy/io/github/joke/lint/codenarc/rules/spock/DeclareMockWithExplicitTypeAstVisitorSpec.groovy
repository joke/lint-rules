package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each method of {@link DeclareMockWithExplicitTypeRule.DeclareMockWithExplicitTypeAstVisitor} is tested directly.
 * The visitor is spied so the method under test runs for real while the siblings it calls are stubbed. The
 * factory call is built as nodes, and {@link MockCall} reads the real node.
 */
@Tag('unit')
class DeclareMockWithExplicitTypeAstVisitorSpec extends Specification {

    static final String DYNAMIC_MESSAGE =
            "Declare the collaborator's type on the left: 'Type name = Mock()' rather than 'def name = Mock(Type)'."
    static final String REPEATED_MESSAGE =
            "Drop the type argument: the declared type already says it, so write 'Type name = Mock()'."

    DeclareMockWithExplicitTypeRule.DeclareMockWithExplicitTypeAstVisitor visitor = Spy()

    def 'visitDeclarationExpression reports the local declaration it is given, then visits its parts'() {
        DeclarationExpression expression = Mock()
        Expression left = Mock()
        Expression right = Mock()

        when:
        visitor.visitDeclarationExpression(expression)

        then:
        1 * visitor.reportUntypedLocal(expression) >> {}
        1 * visitor.visitBinaryExpression(expression) >> { callRealMethod() }
        1 * visitor.visitAnnotations(expression) >> {}
        1 * expression.leftExpression >> left
        1 * left.visit(visitor)
        1 * expression.rightExpression >> right
        1 * right.visit(visitor)
        1 * visitor._
        0 * _
    }

    def 'visitField reports the field it is given, then visits its initialiser'() {
        def initialiser = new ConstantExpression(1)
        FieldNode field = new FieldNode('repository', 0, new ClassNode('Repository', 0, null), null, initialiser)

        when:
        visitor.visitField(field)

        then:
        1 * visitor.reportUntypedField(field) >> {}
        1 * visitor.visitAnnotations(field) >> {}
        1 * visitor.visitConstantExpression(initialiser) >> {}
        1 * visitor._
        0 * _
    }

    def 'reportUntypedLocal reports a first-visited declaration of a variable, with whether it is dynamically typed'() {
        def variable = candidate
        def initialiser = call('Mock')
        def expression = declaration(variable, initialiser)

        when:
        visitor.reportUntypedLocal(expression)

        then:
        1 * visitor.isFirstVisit(expression) >> true
        1 * visitor.reportDeclaration(expression, variable.originType, variable.dynamicTyped, initialiser) >> {}
        1 * visitor._
        0 * _

        where:
        candidate << [
                new VariableExpression('repository'),
                new VariableExpression('repository', new ClassNode('Repository', 0, null)),
        ]
    }

    def 'reportUntypedLocal reports nothing for a declaration already visited'() {
        def expression = declaration(new VariableExpression('repository'), call('Mock'))

        when:
        visitor.reportUntypedLocal(expression)

        then:
        1 * visitor.isFirstVisit(expression) >> false
        1 * visitor._
        0 * _
    }

    def 'reportUntypedLocal reports nothing for a declaration of a tuple, which declares no single type'() {
        def expression = declaration(new TupleExpression(new VariableExpression('a'), new VariableExpression('b')),
                call('Mock'))

        when:
        visitor.reportUntypedLocal(expression)

        then:
        1 * visitor.isFirstVisit(expression) >> true
        1 * visitor._
        0 * _
    }

    def 'reportUntypedField reads the declared type, and whether the field is dynamically typed'() {
        def declared = new ClassNode('Repository', 0, null)
        def initialiser = call('Mock')
        FieldNode field = Mock()

        when:
        visitor.reportUntypedField(field)

        then:
        1 * field.originType >> declared
        1 * field.dynamicTyped >> dynamic
        1 * field.initialExpression >> initialiser
        1 * visitor.reportDeclaration(field, declared, dynamic, initialiser) >> {}
        1 * visitor._
        0 * _

        where:
        dynamic << [true, false]
    }

    def 'reportDeclaration hands a factory call to reportCall, and nothing else'() {
        def node = new ConstantExpression('node')
        def declared = new ClassNode('Repository', 0, null)

        when:
        visitor.reportDeclaration(node, declared, true, initialiser)

        then:
        (factory ? 1 : 0) * visitor.reportCall(node, { it instanceof MockCall }, declared, true) >> {}
        1 * visitor._
        0 * _

        where:
        initialiser                 | factory
        call('Mock')                | true
        new ConstantExpression(1)   | false
    }

    def 'reportCall reports a dynamically typed call, and one whose type argument repeats the declared type'() {
        def node = new ConstantExpression('node')
        def declared = new ClassNode('Repository', 0, null)

        when:
        visitor.reportCall(node, new MockCall(call('Mock', variable(argument))), declared, dynamic)

        then:
        (dynamic ? 1 : 0) * visitor.addViolation(node, DYNAMIC_MESSAGE) >> {}
        (!dynamic && repeats ? 1 : 0) * visitor.addViolation(node, REPEATED_MESSAGE) >> {}
        1 * visitor._
        0 * _

        where:
        dynamic | argument     | repeats
        true    | 'Repository' | false
        false   | 'Repository' | true
        false   | 'Other'      | false
    }

    def 'reportCall reports nothing for a typed declaration with no type argument'() {
        def node = new ConstantExpression('node')

        when:
        visitor.reportCall(node, new MockCall(call('Mock')), new ClassNode('Repository', 0, null), false)

        then:
        1 * visitor._
        0 * _
    }

    private static DeclarationExpression declaration(Expression variable, Expression initialiser) {
        new DeclarationExpression(variable, Token.newSymbol(Types.ASSIGN, 1, 1), initialiser)
    }

    private static VariableExpression variable(String type) {
        new VariableExpression(type)
    }

    private static MethodCallExpression call(String name, Expression... arguments) {
        new MethodCallExpression(new VariableExpression('this'), name, new ArgumentListExpression(arguments))
    }
}
