package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each method of {@link RequireSpyInteractionResponseRule.RequireSpyInteractionResponseAstVisitor} is tested
 * directly. The visitor is spied where its siblings are stubbed. Where the decision runs the scope and the
 * judgement together, a recording subclass captures the violations it is given.
 */
@Tag('unit')
class RequireSpyInteractionResponseAstVisitorSpec extends Specification {

    static final String SPY_MESSAGE =
            "State the response of this interaction on spy 'service': without '>>' it calls the real method. Use '>> value', or '>> { callRealMethod() }' if that is meant."

    RequireSpyInteractionResponseRule.RequireSpyInteractionResponseAstVisitor visitor = Spy()
    Recording recording = new Recording()

    def 'visitMethodEx enters the scope of its class, then judges the statements of the feature method'() {
        def scope = new ClassNode('ExampleSpec', 0, null)
        scope.addField(new FieldNode('service', 0, ClassHelper.DYNAMIC_TYPE, null, call('Spy')))
        Recording recording = new Recording(scope: scope)
        Statement statement = interaction(call('run'))
        statement.addStatementLabel('then')
        def method = new MethodNode('feature', 0, ClassHelper.VOID_TYPE, [] as Parameter[], [] as ClassNode[],
                new BlockStatement([statement], null))

        when:
        recording.visitMethodEx(method)

        then:
        0 * _

        expect:
        recording.reported == [[statement, SPY_MESSAGE]]
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

    def 'judge reports a counted interaction on a spy, with the spy named in the message'() {
        Statement statement = interaction(call('run'))
        recording.visitBlock('given', [declaration('service', call('Spy'))])

        when:
        recording.judge(statement)

        then:
        0 * _

        expect:
        recording.reported == [[statement, SPY_MESSAGE]]
    }

    def 'judge reports nothing for an interaction on a double that is not a spy'() {
        Statement statement = interaction(call('run'))
        recording.visitBlock('given', [declaration('service', call('Mock'))])

        when:
        recording.judge(statement)

        then:
        0 * _

        expect:
        recording.reported.empty
    }

    def 'judge reports nothing for a spy interaction that states its response'() {
        Statement statement = new ExpressionStatement(new BinaryExpression(
                interaction(call('run')).expression, Token.newSymbol('>>', 1, 1), new ConstantExpression('done')))
        recording.visitBlock('given', [declaration('service', call('Spy'))])

        when:
        recording.judge(statement)

        then:
        0 * _

        expect:
        recording.reported.empty
    }

    def 'isUnanswered is true for a counted interaction that states no response'() {
        expect:
        visitor.isUnanswered(new SpockInteraction(expression)) == expected

        where:
        expression                                                                | expected
        interaction(call('run')).expression                                                  | true
        interaction(call('run'), wildcard()).expression                                      | true
        interaction(call('run'), count(0)).expression                                        | false
        interaction(new PropertyExpression(new VariableExpression('service'), '_')).expression | false
        new BinaryExpression(interaction(call('run')).expression, Token.newSymbol('>>', 1, 1), new ConstantExpression('done')) | false
    }

    def 'interactionOf is the interaction an expression statement carries, and nothing for other statements'() {
        expect:
        visitor.interactionOf(interaction(call('run'))).present
        !visitor.interactionOf(new EmptyStatement()).present
    }

    def 'messageFor names the spy in the message'() {
        expect:
        visitor.messageFor('service') == SPY_MESSAGE
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
        new MethodCallExpression(new VariableExpression('service'), name, new ArgumentListExpression())
    }

    /** Records each violation the visitor adds, so a judgement can be read without a rule context. */
    static class Recording extends RequireSpyInteractionResponseRule.RequireSpyInteractionResponseAstVisitor {

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
