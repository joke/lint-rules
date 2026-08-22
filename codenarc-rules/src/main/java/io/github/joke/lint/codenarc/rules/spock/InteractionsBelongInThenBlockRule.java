package io.github.joke.lint.codenarc.rules.spock;

import java.util.List;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a Spock interaction outside a {@code then:} block.
 *
 * <pre>{@code
 * repository.findById('cust-1') >> customer   // reported — reads as configuration
 *
 * when:
 * def receipt = service.checkout(order)
 *
 * then:
 * 1 * repository.save(customer) >> customer   // compliant — verification, next to what caused it
 * 0 * _
 * }</pre>
 *
 * <p>An interaction declares what the subject must call. Placed above {@code when:} it reads as
 * configuration rather than verification and sits away from the action it answers; placed in {@code
 * expect:} it mixes the collaboration contract into the value assertions.
 *
 * <p>{@code and:} continues the block it follows, so an interaction in an {@code and:} after {@code
 * then:} is compliant and the same {@code and:} after {@code when:} is not.
 */
public class InteractionsBelongInThenBlockRule extends AbstractSpockRule {

    private static final String RULE_NAME = "InteractionsBelongInThenBlock";
    private static final int DEFAULT_PRIORITY = 2;

    public InteractionsBelongInThenBlockRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return InteractionsBelongInThenBlockAstVisitor.class;
    }

    public static class InteractionsBelongInThenBlockAstVisitor
            extends AbstractSpockBlockVisitor<InteractionsBelongInThenBlockRule> {

        private static final String MESSAGE =
                "Declare the interaction in 'then:', where it verifies the 'when:' that caused it.";

        @Override
        public void visitBlock(final String label, final List<Statement> statements) {
            if (!THEN_LABEL.equals(label)) {
                statements.forEach(this::reportInteraction);
            }
        }

        @VisibleForTesting
        void reportInteraction(final Statement statement) {
            if (isInteraction(statement)) {
                addViolation(statement, MESSAGE);
            }
        }

        @VisibleForTesting
        boolean isInteraction(final Statement statement) {
            return statement instanceof ExpressionStatement
                    && new SpockInteraction(((ExpressionStatement) statement).getExpression()).isPresent();
        }
    }
}
