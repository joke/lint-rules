package io.github.joke.lint.codenarc.rules.spock;

import java.util.List;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a {@code then:} block whose last statement is not {@code 0 * _}.
 *
 * <pre>{@code
 * then:
 * 1 * repository.save(customer) >> customer
 * 0 * _
 * }</pre>
 *
 * <p>{@code 0 * _} asserts that no interaction other than the declared ones happened on any double.
 * Without it a specification silently tolerates an extra call, which is the regression strict mocking
 * exists to catch: an unplanned collaborator call should break a test until someone declares it on
 * purpose.
 *
 * <p><strong>The terminator is required unconditionally</strong>, whether or not the feature method
 * declares a {@code Mock}, {@code Stub} or {@code Spy}. Requiring it only when a double is in scope
 * was rejected on the failing case: a specification with no collaborators today acquires one the
 * moment the subject grows a dependency, and a conditional rule goes quiet exactly then. So a {@code
 * then:} block whose only statement is the terminator is a valid and expected shape:
 *
 * <pre>{@code
 * when:
 * rule.name = 'Renamed'
 *
 * then:
 * 0 * _
 *
 * expect:
 * rule.name == 'Renamed'
 * }</pre>
 *
 * <p>A {@code then:} and every {@code and:} following it are one run, terminated once at the end
 * rather than once per {@code and:} — requiring one per {@code and:} would assert "nothing else
 * happened" in the middle of a list still being declared.
 *
 * <p>Only {@code then:} is asked for a terminator. A feature method with no collaborators and a
 * standalone {@code expect:} is a documented shape, and demanding a {@code then:} block there would
 * demand a block with nothing to verify.
 */
public class RequireStrictMockingTerminatorRule extends AbstractSpockRule {

    private static final String RULE_NAME = "RequireStrictMockingTerminator";
    private static final int DEFAULT_PRIORITY = 2;

    public RequireStrictMockingTerminatorRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return RequireStrictMockingTerminatorAstVisitor.class;
    }

    public static class RequireStrictMockingTerminatorAstVisitor
            extends AbstractSpockBlockVisitor<RequireStrictMockingTerminatorRule> {

        private static final String MESSAGE =
                "End 'then:' with '0 * _', so an interaction nobody declared fails the specification.";

        /**
         * The last statement of the run is both what is examined and what is reported, so a violation
         * points at the line the terminator should follow.
         *
         * <p>A {@code then:} run is never empty. A label with no statements of its own stacks onto the
         * next labelled statement, and the nearest label wins there — so an empty {@code then:}
         * disappears into the block that follows it rather than arriving here.
         */
        @Override
        public void visitBlock(final String label, final List<Statement> statements) {
            if (THEN_LABEL.equals(label)) {
                reportMissingTerminator(statements.get(statements.size() - 1));
            }
        }

        @VisibleForTesting
        void reportMissingTerminator(final Statement statement) {
            if (!isTerminator(statement)) {
                addViolation(statement, MESSAGE);
            }
        }

        @VisibleForTesting
        boolean isTerminator(final Statement statement) {
            return statement instanceof ExpressionStatement
                    && new SpockInteraction(((ExpressionStatement) statement).getExpression()).isTerminator();
        }
    }
}
