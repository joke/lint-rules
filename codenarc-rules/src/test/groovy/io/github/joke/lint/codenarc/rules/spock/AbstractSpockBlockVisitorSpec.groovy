package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codenarc.rule.junit.SpockUtil
import spock.lang.Specification
import spock.lang.Tag

/**
 * {@link AbstractSpockBlockVisitor} partitions a feature method into its labelled blocks. The partition is
 * observed through a visitor that records each block it is given, so no rule is involved.
 */
@Tag('unit')
class AbstractSpockBlockVisitorSpec extends Specification {

    Recording visitor = new Recording()

    def 'a method whose statements carry no label is one implicit setup block'() {
        def statements = [statement(), statement()]

        when:
        visitor.visitBlocks(statements)

        then:
        0 * _

        expect:
        visitor.blocks == [['setup', statements]]
    }

    def 'each labelled statement starts a block, after the implicit setup region'() {
        def setup = statement()
        def when = statement('when')
        def then = statement('then')

        when:
        visitor.visitBlocks([setup, when, then])

        then:
        0 * _

        expect:
        visitor.blocks.collect { it[0] } == ['setup', 'when', 'then']
        visitor.blocks.collect { it[1].size() } == [1, 1, 1]
    }

    def 'a statement with no label takes the block of the label before it'() {
        def first = statement('when')
        def second = statement()

        when:
        visitor.visitBlocks([first, second])

        then:
        0 * _

        expect:
        visitor.blocks == [['setup', []], ['when', [first, second]]]
    }

    def 'labelOf is empty for a statement with no labels'() {
        expect:
        !visitor.labelOf(statement()).present
    }

    def 'labelOf is the first label in the Spock vocabulary, skipping a loop label'() {
        expect:
        visitor.labelOf(labelled(labels)) == expected

        where:
        labels           | expected
        ['when']         | Optional.of('when')
        ['outer', 'then'] | Optional.of('then')
        ['then', 'when'] | Optional.of('then')
        ['outer']        | Optional.empty()
        ['and']          | Optional.empty()
    }

    def 'visitMethodEx visits the blocks of a feature method'() {
        SpyStatic(SpockUtil)
        def first = statement('given')
        def method = new MethodNode('feature', 0, ClassHelper.VOID_TYPE, [] as Parameter[], [] as ClassNode[],
                new BlockStatement([first], null))

        when:
        visitor.visitMethodEx(method)

        then:
        1 * SpockUtil.isSpockFeatureMethod(method) >> true
        0 * _

        expect:
        visitor.blocks == [['setup', []], ['given', [first]]]
    }

    def 'visitMethodEx skips a method that is not a feature method'() {
        SpyStatic(SpockUtil)
        def method = new MethodNode('fixture', 0, ClassHelper.VOID_TYPE, [] as Parameter[], [] as ClassNode[],
                new BlockStatement([statement()], null))

        when:
        visitor.visitMethodEx(method)

        then:
        1 * SpockUtil.isSpockFeatureMethod(method) >> false
        0 * _

        expect:
        visitor.blocks.empty
    }

    private static ExpressionStatement statement(String label = null) {
        def statement = new ExpressionStatement(new ConstantExpression('x'))
        if (label != null) {
            statement.addStatementLabel(label)
        }
        statement
    }

    private static ExpressionStatement labelled(List<String> labels) {
        def statement = new ExpressionStatement(new ConstantExpression('x'))
        labels.each { statement.addStatementLabel(it) }
        statement
    }

    /** Records each block the visitor is given, in the order it is given them. */
    static class Recording extends AbstractSpockBlockVisitor<AbstractSpockRule> {

        final List<List> blocks = []

        @Override
        void visitBlock(String label, List<Statement> statements) {
            blocks << [label, statements]
        }
    }
}
