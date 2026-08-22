package io.github.joke.lint.codenarc.rules.spock;

import static org.codenarc.rule.junit.SpockUtil.getClosureArgument;

import java.util.Set;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codenarc.rule.AbstractAstVisitor;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a call to {@code Mock}, {@code Stub} or {@code Spy} whose last argument is a closure.
 *
 * <pre>{@code
 * CustomerRepository repository = Mock() {       // reported
 *     findById(_) >> customer
 * }
 * }</pre>
 *
 * <p>An interaction declared in the initialiser sits away from the {@code when:} it answers and
 * reads as configuration rather than as verification. It also loses its cardinality: the initialiser
 * form stubs a return without asserting that the call happened, which is the half of the contract
 * worth having.
 *
 * <p>{@code Spy(constructorArgs: [repository])} passes a map rather than a closure and is the
 * documented way to construct a spy over a real object, so it is not reported.
 */
public class AvoidMockInitializerClosureRule extends AbstractSpockRule {

    private static final String RULE_NAME = "AvoidMockInitializerClosure";
    private static final int DEFAULT_PRIORITY = 2;

    public AvoidMockInitializerClosureRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return AvoidMockInitializerClosureAstVisitor.class;
    }

    public static class AvoidMockInitializerClosureAstVisitor
            extends AbstractAstVisitor<AvoidMockInitializerClosureRule> {

        private static final Set<String> MOCK_FACTORIES = Set.of("Mock", "Stub", "Spy");

        private static final String MESSAGE =
                "Declare the interaction in 'then:' with its cardinality and its real argument, not in the mock's initialiser.";

        @Override
        public void visitMethodCallExpression(final MethodCallExpression call) {
            reportInitializerClosure(call);
            super.visitMethodCallExpression(call);
        }

        /**
         * {@code isFirstVisit} because a non-private field is both a property and a field, and
         * CodeNarc traverses the initialiser of each — so a mock field's call is reached twice and
         * would otherwise be reported twice for one line.
         */
        @VisibleForTesting
        void reportInitializerClosure(final MethodCallExpression call) {
            if (isFirstVisit(call) && isMockFactory(call) && hasClosureArgument(call)) {
                addViolation(call, MESSAGE);
            }
        }

        /**
         * Matched by name only, for the reason {@link DeclareMockWithExplicitTypeRule} gives: the
         * call arrives as an implicit-{@code this} invocation, resolving it would need a compile
         * classpath, and the class gate is what keeps the bare name from matching elsewhere.
         *
         * <p>Compared through the set rather than {@code contains}, because {@code getMethodAsString}
         * is null for a dynamically-named call and {@link Set#of} throws on a null lookup.
         */
        @VisibleForTesting
        boolean isMockFactory(final MethodCallExpression call) {
            return MOCK_FACTORIES.stream().anyMatch(factory -> factory.equals(call.getMethodAsString()));
        }

        @VisibleForTesting
        boolean hasClosureArgument(final MethodCallExpression call) {
            return getClosureArgument(call) != null;
        }
    }
}
