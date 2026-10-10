package io.github.joke.lint.codenarc.rules.spock;

import java.util.List;
import java.util.Optional;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports an interaction on a {@code Stub} that carries a cardinality other than {@code _}.
 *
 * <pre>{@code
 * 1 * repository.find(id)                   // reported
 * 1 * repository.find(id) >> customer       // reported — the response does not remove the count
 * 0 * repository.find(id)                   // reported — Spock treats it as required too
 * repository.find(id) >> customer           // compliant
 * _ * repository.find(id) >> customer       // compliant — not a required interaction
 * 0 * _                                     // compliant — names no stub
 * }</pre>
 *
 * <p>A stub is not verified, and Spock rejects an interaction that is required on one with {@code
 * InvalidSpecException: Stub '…' matches the following required interaction}. An interaction is
 * required unless its cardinality is unbounded, which is why even {@code 0 *} is reported. A double
 * whose calls are counted is a {@code Mock}.
 *
 * <p>The rule restates a failure the specification would have at run time, at the line that causes
 * it. The forms were established against Spock 2.4, not inferred.
 *
 * <p>A stub is recognised from its declaration, as the spy rules recognise a spy, and a {@code Stub}
 * call that passes {@code verified} is not one: {@code Stub(verified: true)} is allowed a required
 * interaction. A {@code Mock} and a {@code Spy} are verified and accept a cardinality, so neither is
 * in scope, and neither is a stub from a helper method, a base class or a parameter. {@code
 * GroovyStub} is not recognised either: the factory set the family reads does not include it.
 * Interactions inside an {@code interaction { }} closure are not visited, the boundary the argument
 * rule documents.
 */
public class AvoidCardinalityOnStubRule extends AbstractSpockRule {

    private static final String RULE_NAME = "AvoidCardinalityOnStub";
    private static final int DEFAULT_PRIORITY = 2;

    public AvoidCardinalityOnStubRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return AvoidCardinalityOnStubAstVisitor.class;
    }

    public static class AvoidCardinalityOnStubAstVisitor extends AbstractSpockBlockVisitor<AvoidCardinalityOnStubRule> {

        private final DoubleScope stubs = new DoubleScope(MockCall::isUnverifiedStub);

        @Override
        public void visitMethodEx(final MethodNode node) {
            stubs.enter(getCurrentClassNode());
            super.visitMethodEx(node);
        }

        /**
         * Locals are collected before the block is judged, because a declaration always precedes the
         * statement that uses it and an earlier block's stubs stay in scope for the later ones.
         */
        @Override
        public void visitBlock(final String label, final List<Statement> statements) {
            stubs.collectLocals(statements);
            statements.forEach(this::judge);
        }

        @VisibleForTesting
        void judge(final Statement statement) {
            interactionOf(statement)
                    .filter(SpockInteraction::isRequired)
                    .flatMap(SpockInteraction::getTarget)
                    .flatMap(stubs::doubleNamedBy)
                    .ifPresent(stub -> addViolation(statement, messageFor(stub)));
        }

        @VisibleForTesting
        Optional<SpockInteraction> interactionOf(final Statement statement) {
            return statement instanceof ExpressionStatement
                    ? Optional.of(new SpockInteraction(((ExpressionStatement) statement).getExpression()))
                    : Optional.empty();
        }

        @VisibleForTesting
        String messageFor(final String stub) {
            return "Remove the cardinality from this interaction on stub '" + stub
                    + "': a stub is not verified, and Spock rejects a required interaction on one. Use '>> value', or make it a Mock.";
        }
    }
}
