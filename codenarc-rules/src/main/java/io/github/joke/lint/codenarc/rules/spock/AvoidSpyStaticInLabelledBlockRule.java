package io.github.joke.lint.codenarc.rules.spock;

import java.util.List;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a call to {@code SpyStatic} under any Spock statement label inside a feature method.
 *
 * <pre>{@code
 * def 'applies the configured regional tax rate'() {
 *     SpyStatic(PricingRules)                 // compliant — unlabelled setup
 *     def service = new InvoiceService()
 *
 *     when:
 *     def total = service.totalWithTax(100G, 'eu')
 *
 *     then:
 *     1 * PricingRules.taxRate('eu') >> 0.20G
 *     0 * _
 * }
 * }</pre>
 *
 * <p>{@code SpyStatic} enables static mocking for the whole feature method. It belongs with the other
 * unlabelled setup statements at the top, where the reader looks for what the method arranges. Placed
 * in {@code then:} it sits among interaction verifications and reads as one, which it is not — it
 * declares nothing about what was called.
 *
 * <p>Matched on the name as written, with no attempt to resolve it — the same choice {@link
 * AvoidUnrollAnnotationRule} makes for {@code @Unroll}, and for the same reason: CodeNarc analyses
 * source without a compile classpath, so name-matching is the only mechanism available. The class
 * gate is what keeps the bare name from matching outside a specification.
 *
 * <p>{@code SpyStatic} is real API here — {@code spock.mock.MockingApi} declares it, {@code
 * SpecInternals.SpyStaticImpl} implements it and Spock's own {@code Identifiers} lists it — but no
 * specification in this artifact's corpus calls it. The rule's fixtures are therefore its only
 * evidence, which is why it carries the lowest priority of the rules shipped alongside it.
 */
public class AvoidSpyStaticInLabelledBlockRule extends AbstractSpockRule {

    private static final String RULE_NAME = "AvoidSpyStaticInLabelledBlock";
    private static final int DEFAULT_PRIORITY = 3;

    public AvoidSpyStaticInLabelledBlockRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return AvoidSpyStaticInLabelledBlockAstVisitor.class;
    }

    public static class AvoidSpyStaticInLabelledBlockAstVisitor
            extends AbstractSpockBlockVisitor<AvoidSpyStaticInLabelledBlockRule> {

        private static final String SPY_STATIC = "SpyStatic";

        private static final String MESSAGE =
                "Call 'SpyStatic' with the unlabelled setup statements: it arranges the whole feature method rather than verifying one call.";

        /**
         * The label alone cannot answer this, because the implicit setup region arrives under {@code
         * setup} exactly as a written {@code setup:} label does. What separates them is that the
         * implicit region's first statement carries no Spock label — a labelled statement is what
         * opens the run it labels — so the run is asked rather than its name.
         */
        @Override
        public void visitBlock(final String label, final List<Statement> statements) {
            if (isLabelled(statements)) {
                statements.forEach(this::reportSpyStatic);
            }
        }

        /** The implicit setup region is empty when the feature method opens with a label. */
        @VisibleForTesting
        boolean isLabelled(final List<Statement> statements) {
            return !statements.isEmpty() && labelOf(statements.get(0)).isPresent();
        }

        @VisibleForTesting
        void reportSpyStatic(final Statement statement) {
            if (statement instanceof ExpressionStatement
                    && isSpyStaticCall(((ExpressionStatement) statement).getExpression())) {
                addViolation(statement, MESSAGE);
            }
        }

        /**
         * The whole name is compared, so a call whose name merely ends in {@code SpyStatic} is not
         * matched.
         */
        @VisibleForTesting
        boolean isSpyStaticCall(final Expression expression) {
            return expression instanceof MethodCallExpression
                    && SPY_STATIC.equals(((MethodCallExpression) expression).getMethodAsString());
        }
    }
}
