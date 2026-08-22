package io.github.joke.lint.codenarc.rules.spock;

import static org.codenarc.rule.junit.SpockUtil.isBooleanExpression;

import java.util.List;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a boolean expression in a {@code then:} block.
 *
 * <pre>{@code
 * then:
 * 1 * repository.save(customer) >> customer
 * receipt.total == 49.99G                     // reported — belongs in expect:
 * 0 * _
 *
 * expect:
 * receipt.total == 49.99G
 * }</pre>
 *
 * <p>{@code then:} states who the subject called; {@code expect:} states what it returned. A {@code
 * then:} block full of {@code ==} buries the interaction contract in the middle of value checks, and
 * the contract is the half a reader most needs to find.
 *
 * <p>What counts as a value assertion is CodeNarc's own boolean-expression test rather than
 * "everything that is not an interaction". Sharing that judgement means this rule and {@code
 * SpockMissingAssert} agree by construction about one line, where the negative definition would
 * report an ordinary helper call in a {@code then:} block — a false positive on code that is not
 * wrong.
 *
 * <p>No carve-out is needed for what stays: an interaction is a {@code BinaryExpression} carrying
 * {@code *} or {@code >>}, which the test does not recognise as boolean; {@code thrown(…)} and {@code
 * notThrown(…)} match none of its boolean-name patterns; and {@code def error = thrown(…)} is a
 * declaration.
 *
 * <p>The cost is stated rather than worked around: an expression the test does not recognise as
 * boolean is not reported, and a bare truthiness check such as {@code receipt.valid} is the known
 * case. The rule under-reports rather than guesses.
 */
public class ValueAssertionsBelongInExpectBlockRule extends AbstractSpockRule {

    private static final String RULE_NAME = "ValueAssertionsBelongInExpectBlock";
    private static final int DEFAULT_PRIORITY = 2;

    public ValueAssertionsBelongInExpectBlockRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return ValueAssertionsBelongInExpectBlockAstVisitor.class;
    }

    public static class ValueAssertionsBelongInExpectBlockAstVisitor
            extends AbstractSpockBlockVisitor<ValueAssertionsBelongInExpectBlockRule> {

        private static final String MESSAGE = "'then:' states who was called; move the value assertion to 'expect:'.";

        @Override
        public void visitBlock(final String label, final List<Statement> statements) {
            if (THEN_LABEL.equals(label)) {
                statements.forEach(this::reportValueAssertion);
            }
        }

        @VisibleForTesting
        void reportValueAssertion(final Statement statement) {
            if (isValueAssertion(statement)) {
                addViolation(statement, MESSAGE);
            }
        }

        @VisibleForTesting
        boolean isValueAssertion(final Statement statement) {
            return statement instanceof ExpressionStatement && isBooleanExpression((ExpressionStatement) statement);
        }
    }
}
