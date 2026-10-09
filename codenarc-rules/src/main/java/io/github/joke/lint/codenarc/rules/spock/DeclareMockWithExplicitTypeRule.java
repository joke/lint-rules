package io.github.joke.lint.codenarc.rules.spock;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codenarc.rule.AbstractAstVisitor;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a dynamically-typed declaration whose initialiser is a call to {@code Mock}, {@code Stub}
 * or {@code Spy}.
 *
 * <pre>{@code
 * def repository = Mock(CustomerRepository)      // reported
 * CustomerRepository repository = Mock()         // compliant
 * }</pre>
 *
 * <p>The type is written once. A typed declaration that repeats it as the factory's argument —
 * {@code TypeMirror mirror = Stub(TypeMirror)} — is reported too, because Spock infers the double's
 * type from the variable's. Only the <em>same</em> type is reported: {@code Collection<String> items
 * = Mock(List)} carries information the declared type cannot, and removing the argument would change
 * what is created.
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

        private final MockFactories factories = new MockFactories();

        private static final String MESSAGE =
                "Declare the collaborator's type on the left: 'Type name = Mock()' rather than 'def name = Mock(Type)'.";

        private static final String REPEATED_MESSAGE =
                "Drop the type argument: the declared type already says it, so write 'Type name = Mock()'.";

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
            if (isFirstVisit(expression) && expression.getLeftExpression() instanceof VariableExpression) {
                final var variable = (VariableExpression) expression.getLeftExpression();
                reportDeclaration(
                        expression,
                        variable.getOriginType(),
                        variable.isDynamicTyped(),
                        expression.getRightExpression());
            }
        }

        @VisibleForTesting
        void reportUntypedField(final FieldNode node) {
            reportDeclaration(node, node.getOriginType(), node.isDynamicTyped(), node.getInitialExpression());
        }

        @VisibleForTesting
        void reportDeclaration(
                final ASTNode node, final ClassNode declared, final boolean dynamic, final Expression initialiser) {
            factories.read(initialiser).ifPresent(call -> reportCall(node, call, declared, dynamic));
        }

        /**
         * An untyped declaration is reported for what it hides, a typed one for what it repeats. The
         * two are exclusive: a variable is either dynamically typed or it is not.
         */
        @VisibleForTesting
        void reportCall(final ASTNode node, final MockCall call, final ClassNode declared, final boolean dynamic) {
            if (dynamic) {
                addViolation(node, MESSAGE);
            } else if (call.repeats(declared)) {
                addViolation(node, REPEATED_MESSAGE);
            }
        }
    }
}
