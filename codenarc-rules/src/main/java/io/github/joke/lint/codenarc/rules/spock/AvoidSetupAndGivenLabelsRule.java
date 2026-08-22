package io.github.joke.lint.codenarc.rules.spock;

import static org.codenarc.rule.junit.SpockUtil.isSpockFeatureMethod;

import java.util.List;
import java.util.Set;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AbstractAstVisitor;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a {@code setup:} or {@code given:} statement label inside a Spock feature method.
 *
 * <p>Spock treats unlabelled statements at the top of a feature method as the implicit setup block,
 * so the label states what the statement's position already says. Removed, the blank line before
 * {@code when:} carries the boundary.
 *
 * <p>{@code when:}, {@code then:}, {@code expect:}, {@code where:}, {@code cleanup:}, {@code and:},
 * {@code filter:} and {@code combined:} are untouched. {@code and:} is a continuation with no
 * semantic weight, and {@code cleanup:} and {@code where:} have no unlabelled equivalent — the label
 * is the only way to declare them — so reporting them would demand a rewrite that does not exist.
 */
public class AvoidSetupAndGivenLabelsRule extends AbstractSpockRule {

    private static final String RULE_NAME = "AvoidSetupAndGivenLabels";
    private static final int DEFAULT_PRIORITY = 2;

    public AvoidSetupAndGivenLabelsRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return AvoidSetupAndGivenLabelsAstVisitor.class;
    }

    public static class AvoidSetupAndGivenLabelsAstVisitor extends AbstractAstVisitor<AvoidSetupAndGivenLabelsRule> {

        private static final Set<String> REPORTED_LABELS = Set.of("setup", "given");

        /**
         * A fixture method is not a feature method. {@code isSpockFeatureMethod} matches Spock's own
         * {@code SpecParser} — a method carrying at least one Spock statement label — so a {@code
         * def setup()} or {@code def setupSpec()} carrying none is skipped, which is what keeps this
         * rule off the fixture methods whose name it shares.
         */
        @Override
        public void visitMethodEx(final MethodNode node) {
            if (isSpockFeatureMethod(node)) {
                reportLabelsIn(node);
            }
        }

        /**
         * The cast is safe: {@code isSpockFeatureMethod} returns true only for a method whose code
         * is a {@link BlockStatement}. Only the method's own statements are walked, because that is
         * where Spock reads its labels from.
         */
        @VisibleForTesting
        void reportLabelsIn(final MethodNode node) {
            ((BlockStatement) node.getCode()).getStatements().forEach(this::reportLabelsOn);
        }

        /**
         * Distinct, because a label introducing an empty block is attached by Groovy to the
         * following statement, so one statement can carry several — and the same one twice.
         */
        @VisibleForTesting
        void reportLabelsOn(final Statement statement) {
            labelsOf(statement).stream()
                    .distinct()
                    .filter(REPORTED_LABELS::contains)
                    .forEach(label -> report(statement, label));
        }

        @VisibleForTesting
        List<String> labelsOf(final Statement statement) {
            final var labels = statement.getStatementLabels();
            return labels == null ? List.of() : labels;
        }

        @VisibleForTesting
        void report(final Statement statement, final String label) {
            addViolation(
                    statement,
                    "Spock treats unlabelled statements at the top of a feature method as the setup block, so '" + label
                            + ":' adds nothing.");
        }
    }
}
