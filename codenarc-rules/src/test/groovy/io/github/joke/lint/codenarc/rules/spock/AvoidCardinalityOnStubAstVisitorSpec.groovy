package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each method of {@link AvoidCardinalityOnStubRule.AvoidCardinalityOnStubAstVisitor} is tested directly. The
 * visitor is spied so the method under test runs for real while the siblings it calls are stubbed. The stub
 * scope is the visitor's own, populated through its collection method on real declarations.
 */
@Tag('unit')
class AvoidCardinalityOnStubAstVisitorSpec extends Specification {

    static final String STUB_MESSAGE =
            "Remove the cardinality from this interaction on stub 'repository': a stub is not verified, and Spock rejects a required interaction on one. Use '>> value', or make it a Mock."

    AvoidCardinalityOnStubRule.AvoidCardinalityOnStubAstVisitor visitor = Spy()
    Recording recording = new Recording()

    def 'visitMethodEx enters the scope of its class, then judges the statements of the feature method'() {
        def scope = new ClassNode('ExampleSpec', 0, null)
        scope.addField(new FieldNode('repository', 0, ClassHelper.DYNAMIC_TYPE, null, call('Stub')))
        Recording recording = new Recording(scope: scope)
        Statement statement = interaction(call('find'))
        statement.addStatementLabel('then')
        def method = new MethodNode('feature', 0, ClassHelper.VOID_TYPE, [] as Parameter[], [] as ClassNode[],
                new BlockStatement([statement], null))

        when:
        recording.visitMethodEx(method)

        then:
        0 * _

        expect:
        recording.reported == [[statement, STUB_MESSAGE]]
    }

    def 'visitBlock collects the locals of the block, then judges each statement'() {
        Statement first = statement()
        Statement second = statement()
        def statements = [first, second]

        when:
        visitor.visitBlock('then', statements)

        then:
        1 * visitor.judge(first) >> {}
        1 * visitor.judge(second) >> {}
        1 * visitor._
        0 * _
    }

    def 'judge reports a counted interaction on a stub, with the stub named in the message'() {
        Statement statement = interaction(call('find'))
        recording.visitBlock('given', [declaration('repository', call('Stub'))])

        when:
        recording.judge(statement)

        then:
        0 * _

        expect:
        recording.reported == [[statement, STUB_MESSAGE]]
    }

    def 'judge reports nothing for a required interaction on a double that is not an unverified stub'() {
        Statement statement = interaction(call('find'))
        recording.visitBlock('given', [declaration('repository', call('Mock'))])

        when:
        recording.judge(statement)

        then:
        0 * _

        expect:
        recording.reported.empty
    }

    def 'judge reports nothing for a wildcard count on a stub'() {
        Statement statement = interaction(call('find'), wildcard())
        recording.visitBlock('given', [declaration('repository', call('Stub'))])

        when:
        recording.judge(statement)

        then:
        0 * _

        expect:
        recording.reported.empty
    }

    def 'interactionOf is the interaction an expression statement carries, and nothing for other statements'() {
        expect:
        visitor.interactionOf(interaction(call('find'))).present
        !visitor.interactionOf(new EmptyStatement()).present
    }

    def 'messageFor names the stub in the message'() {
        expect:
        visitor.messageFor('repository') == STUB_MESSAGE
    }

    private static Statement declaration(String name, Expression initialiser) {
        new ExpressionStatement(new DeclarationExpression(new VariableExpression(name),
                Token.newSymbol(Types.ASSIGN, 1, 1), initialiser))
    }

    private static Statement statement() {
        new ExpressionStatement(new ConstantExpression('x'))
    }

    private static Statement interaction(Expression target, Expression count = count(1)) {
        new ExpressionStatement(new BinaryExpression(count, Token.newSymbol('*', 1, 1), target))
    }

    private static ConstantExpression count(int n) {
        new ConstantExpression(n)
    }

    private static VariableExpression wildcard() {
        new VariableExpression('_')
    }

    private static MethodCallExpression call(String name) {
        new MethodCallExpression(new VariableExpression('repository'), name, new ArgumentListExpression())
    }

    private static MethodCallExpression call(String name, Expression... arguments) {
        new MethodCallExpression(new VariableExpression('this'), name, new ArgumentListExpression(arguments))
    }

    /** Records each violation the visitor adds, so a judgement can be read without a rule context. */
    static class Recording extends AvoidCardinalityOnStubRule.AvoidCardinalityOnStubAstVisitor {

        ClassNode scope
        final List<List> reported = []

        @Override
        protected ClassNode getCurrentClassNode() {
            scope
        }

        @Override
        protected void addViolation(ASTNode node, String message) {
            reported << [node, message]
        }
    }
}
