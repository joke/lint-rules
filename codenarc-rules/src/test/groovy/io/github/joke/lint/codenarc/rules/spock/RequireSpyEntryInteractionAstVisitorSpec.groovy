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
import spock.lang.Subject
import spock.lang.Tag

/**
 * Each method of {@link RequireSpyEntryInteractionRule.RequireSpyEntryInteractionAstVisitor} is tested directly.
 * The visitor is spied where a method's siblings are stubbed. Where a decision needs the spies in scope, a
 * recording subclass is given that scope through its visitBlock, so the decision runs for real.
 */
@Tag('unit')
@Subject(RequireSpyEntryInteractionRule.RequireSpyEntryInteractionAstVisitor)
class RequireSpyEntryInteractionAstVisitorSpec extends Specification {

    static final String ENTRY_MISPLACED =
            "Declare '1 * service._' after the specific interactions on the spy: declared first it absorbs the calls they verify."
    static final String ENTRY_MISSING =
            "Add '1 * service._' here, or '0 * _' fails on the call 'when:' made on the spy."

    RequireSpyEntryInteractionRule.RequireSpyEntryInteractionAstVisitor visitor = Spy()
    Recording recording = new Recording()

    def 'visitMethodEx enters the scope of its class, then judges the then block of the feature method'() {
        def scope = new ClassNode('ExampleSpec', 0, null)
        scope.addField(new FieldNode('service', 0, ClassHelper.DYNAMIC_TYPE, null, call('Spy')))
        Recording recording = new Recording(scope: scope)
        Statement specific = interaction(call('run'))
        specific.addStatementLabel('then')
        def method = new MethodNode('feature', 0, ClassHelper.VOID_TYPE, [] as Parameter[], [] as ClassNode[],
                new BlockStatement([specific], null))

        when:
        recording.visitMethodEx(method)

        then:
        0 * _

        expect:
        recording.reported == [[specific, ENTRY_MISSING]]
    }

    def 'visitBlock collects the locals of the block, and judges only a then block'() {
        def statements = [statement(), statement()]

        when:
        visitor.visitBlock(label, statements)

        then:
        (label == 'then' ? 1 : 0) * visitor.judgeRun(statements) >> {}
        1 * visitor._
        0 * _

        where:
        label << ['then', 'when']
    }

    def 'judgeRun judges each spy the run names, in the order it names them'() {
        def statements = [statement(), statement()]

        when:
        visitor.judgeRun(statements)

        then:
        1 * visitor.spiesNamedIn(statements) >> (['service', 'repository'] as LinkedHashSet)
        1 * visitor.judgeSpy('service', statements) >> {}
        1 * visitor.judgeSpy('repository', statements) >> {}
        1 * visitor._
        0 * _
    }

    def 'judgeSpy reports a run whose last interaction on the spy is not the entry interaction'() {
        def statements = [statement(), statement()]
        Statement specific = interaction(call('run'))
        Statement last = interaction(call('find'))

        when:
        visitor.judgeSpy('service', statements)

        then:
        1 * visitor.interactionsOn('service', statements) >> [specific, last]
        1 * visitor.isEntryInteraction(last) >> entry
        (entry ? 0 : 1) * visitor.reportSpy('service', [specific, last], last) >> {}
        1 * visitor._
        0 * _

        where:
        entry << [true, false]
    }

    def 'reportSpy reports a misplaced entry interaction on the entry'() {
        Statement specific = interaction(call('run'))
        Statement entry = interaction(new PropertyExpression(new VariableExpression('service'), '_'))

        when:
        recording.reportSpy('service', [entry, specific], specific)

        then:
        0 * _

        expect:
        recording.reported == [[entry, ENTRY_MISPLACED]]
    }

    def 'reportSpy reports a missing entry interaction on the last specific one'() {
        Statement specific = interaction(call('run'))

        when:
        recording.reportSpy('service', [specific], specific)

        then:
        0 * _

        expect:
        recording.reported == [[specific, ENTRY_MISSING]]
    }

    def 'interactionsOn is the statements that name the spy, in order'() {
        recording.visitBlock('given', [declaration('service', call('Spy')), declaration('probe', call('Spy')),
                declaration('repository', call('Mock'))])
        Statement first = interaction(call('run'))
        Statement other = interaction(new MethodCallExpression(new VariableExpression('probe'), 'find',
                new ArgumentListExpression()))
        Statement second = interaction(call('stop'))

        when:
        def named = recording.interactionsOn('service', [first, other, second])

        then:
        0 * _

        expect:
        named.size() == 2
        named[0].is(first)
        named[1].is(second)
    }

    def 'spiesNamedIn is the spies the statements name, each once, in first-appearance order'() {
        recording.visitBlock('given', [declaration('service', call('Spy')), declaration('probe', call('Spy')),
                declaration('repository', call('Mock'))])
        Statement fromService = interaction(call('run'))
        Statement fromRepository = interaction(new MethodCallExpression(new VariableExpression('repository'), 'find',
                new ArgumentListExpression()))
        Statement fromProbe = interaction(new MethodCallExpression(new VariableExpression('probe'), 'find',
                new ArgumentListExpression()))

        when:
        def spies = recording.spiesNamedIn([fromProbe, fromRepository, fromService, fromProbe, fromService])

        then:
        0 * _

        expect:
        spies == (['probe', 'service'] as LinkedHashSet)
    }

    def 'spyNamedBy is the spy a statement targets, and nothing for a target that is not a spy'() {
        recording.visitBlock('given', [declaration('service', call('Spy')), declaration('repository', call('Mock'))])

        expect:
        recording.spyNamedBy(interaction(call('run'))) == Optional.of('service')
        !recording.spyNamedBy(interaction(new MethodCallExpression(new VariableExpression('repository'), 'find',
                new ArgumentListExpression()))).present
        !recording.spyNamedBy(new EmptyStatement()).present
    }

    def 'targetOf is the expression an interaction constrains, and nothing for a plain statement'() {
        expect:
        visitor.targetOf(interaction(call('run'))).present
        !visitor.targetOf(new ExpressionStatement(call('run'))).present
        !visitor.targetOf(new EmptyStatement()).present
    }

    def 'isEntryInteraction is true only for a property access on the wildcard'() {
        expect:
        visitor.isEntryInteraction(interaction(new PropertyExpression(new VariableExpression('service'), '_')))
        !visitor.isEntryInteraction(interaction(call('run')))
        !visitor.isEntryInteraction(new EmptyStatement())
    }

    def 'expressionOf is the expression of an expression statement, and nothing otherwise'() {
        expect:
        visitor.expressionOf(new ExpressionStatement(call('run'))).present
        !visitor.expressionOf(new EmptyStatement()).present
    }

    private static Statement declaration(String name, Expression initialiser) {
        new ExpressionStatement(new DeclarationExpression(new VariableExpression(name),
                Token.newSymbol(Types.ASSIGN, 1, 1), initialiser))
    }

    private static Statement statement() {
        new ExpressionStatement(new ConstantExpression('x'))
    }

    private static Statement interaction(Expression target) {
        new ExpressionStatement(new BinaryExpression(new ConstantExpression(1), Token.newSymbol('*', 1, 1), target))
    }

    private static MethodCallExpression call(String name) {
        new MethodCallExpression(new VariableExpression('service'), name, new ArgumentListExpression())
    }

    /** Records each violation the visitor adds, and takes its scope from a class node given to it. */
    static class Recording extends RequireSpyEntryInteractionRule.RequireSpyEntryInteractionAstVisitor {

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
