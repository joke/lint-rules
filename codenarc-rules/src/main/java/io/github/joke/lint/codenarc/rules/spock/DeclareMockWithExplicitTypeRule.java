package io.github.joke.lint.codenarc.rules.spock;

import java.util.Set;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codenarc.rule.AbstractAstVisitor;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

/**
 * Reports a dynamically-typed declaration whose initialiser is a call to {@code Mock}, {@code Stub}
 * or {@code Spy}.
 *
 * <pre>{@code
 * def repository = Mock(CustomerRepository)      // reported
 * CustomerRepository repository = Mock()         // compliant
 * }</pre>
 *
 * <p>The declared type is what a reader looks at to learn who the subject collaborates with. Moved
 * into the initialiser it is still present but no longer in the position that answers the question,
 * and the variable itself is untyped for every later line that uses it.
 *
 * <p>What is reported is the <em>declaration</em>, not the call. A mock passed straight to a
 * constructor or a method — {@code new CheckoutService(Mock(PaymentGateway))} — declares no variable
 * whose type could have been written, and is the documented way to supply a collaborator the
 * specification never refers to again.
 */
public class DeclareMockWithExplicitTypeRule extends AbstractSpockRule {

    private static final String RULE_NAME = "DeclareMockWithExplicitType";
    private static final int DEFAULT_PRIORITY = 2;

    public DeclareMockWithExplicitTypeRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return DeclareMockWithExplicitTypeAstVisitor.class;
    }

    public static class DeclareMockWithExplicitTypeAstVisitor
            extends AbstractAstVisitor<DeclareMockWithExplicitTypeRule> {

        private static final Set<String> MOCK_FACTORIES = Set.of("Mock", "Stub", "Spy");

        private static final String MESSAGE =
                "Declare the collaborator's type on the left: 'Type name = Mock()' rather than 'def name = Mock(Type)'.";

        @Override
        public void visitDeclarationExpression(final DeclarationExpression expression) {
            reportUntypedLocal(expression);
            super.visitDeclarationExpression(expression);
        }

        /**
         * Fields are visited separately from locals because they are a different node: a local is a
         * {@link DeclarationExpression} and a field is a {@link FieldNode} carrying an initial
         * expression. Declaring collaborators as fields is the more common Spock form, so a rule
         * visiting only declarations would miss most of what this one is for.
         */
        @Override
        public void visitField(final FieldNode node) {
            reportUntypedField(node);
            super.visitField(node);
        }

        /**
         * A multiple assignment — {@code def (a, b) = pair} — has a tuple on the left rather than a
         * variable, so it declares no single type and is not reported.
         *
         * <p>{@code isFirstVisit} because a non-private field is both a property and a field, and
         * CodeNarc traverses the initialiser of each. A declaration nested inside such an
         * initialiser — {@code def factory = { def repository = Mock(Repo) }} — is therefore reached
         * twice, and without the guard would be reported twice for one line.
         */
        @VisibleForTesting
        void reportUntypedLocal(final DeclarationExpression expression) {
            if (isFirstVisit(expression)
                    && isDynamicVariable(expression.getLeftExpression())
                    && isMockCall(expression.getRightExpression())) {
                addViolation(expression, MESSAGE);
            }
        }

        @VisibleForTesting
        void reportUntypedField(final FieldNode node) {
            if (node.isDynamicTyped() && isMockCall(node.getInitialExpression())) {
                addViolation(node, MESSAGE);
            }
        }

        @VisibleForTesting
        boolean isDynamicVariable(final Expression expression) {
            return expression instanceof VariableExpression && ((VariableExpression) expression).isDynamicTyped();
        }

        /**
         * Matched by name only. The call arrives as an implicit-{@code this} invocation on the
         * specification, but nothing here depends on the receiver: resolving {@code Mock} to Spock's
         * {@code MockingApi} would need a compile classpath CodeNarc does not have, and the class
         * gate is what keeps the bare name from matching outside a specification.
         */
        @VisibleForTesting
        boolean isMockCall(final @Nullable Expression expression) {
            return expression instanceof MethodCallExpression && isMockFactory((MethodCallExpression) expression);
        }

        /**
         * Compared through the set rather than {@code contains}, because {@code getMethodAsString}
         * is null for a dynamically-named call and {@link Set#of} throws on a null lookup.
         */
        @VisibleForTesting
        boolean isMockFactory(final MethodCallExpression call) {
            return MOCK_FACTORIES.stream().anyMatch(factory -> factory.equals(call.getMethodAsString()));
        }
    }
}
