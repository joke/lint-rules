package io.github.joke.lint.codenarc.rules.spock;

import java.util.List;
import java.util.Optional;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a counted interaction on a {@code Spy} that states no response.
 *
 * <pre>{@code
 * 1 * service.validate(order)                          // reported — calls the real method
 * 1 * service.validate(order) >> true                  // compliant
 * 1 * service.validate(order) >> { callRealMethod() }  // compliant — the call-through, said aloud
 * 1 * service._                                        // compliant — the entry interaction
 * }</pre>
 *
 * <p>Without a response, an interaction on a spy calls the <em>real</em> method. That is rarely what
 * {@code 1 * service.validate(order)} reads as, so the call-through is stated, either as a value or
 * closure response or, where the real method is meant, as {@code >> { callRealMethod() }}.
 *
 * <p><strong>Exactly {@code spy._} is exempt.</strong> It is the deliberate, short way to allow the
 * spy's one self-call from {@code when:}, and {@link RequireSpyEntryInteractionRule} requires it. It
 * is the property access, not {@code spy._(argument)} — a filter over every method's argument — and
 * not a pattern-matched method name; either can call real code nobody named. The cardinality on
 * {@code spy._} is not read, as the entry rule does not read it: the two rules must not disagree about
 * {@code 2 * service._}.
 *
 * <p>{@code 0 *} is exempt too: the call is asserted never to happen, so nothing can call through.
 *
 * <p>A spy is recognised from its declaration, as {@link RequireSpyEntryInteractionRule} does it. A
 * {@code Mock} returns a default and a {@code Stub} an empty value, so neither is in scope.
 * Interactions inside an {@code interaction { }} closure are not visited, the boundary the argument
 * rule documents; which block a statement sits in is {@code InteractionsBelongInThenBlock}'s concern.
 */
public class RequireSpyInteractionResponseRule extends AbstractSpockRule {

    private static final String RULE_NAME = "RequireSpyInteractionResponse";
    private static final int DEFAULT_PRIORITY = 2;

    public RequireSpyInteractionResponseRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return RequireSpyInteractionResponseAstVisitor.class;
    }

    public static class RequireSpyInteractionResponseAstVisitor
            extends AbstractSpockBlockVisitor<RequireSpyInteractionResponseRule> {

        private final SpyScope spies = new SpyScope();

        @Override
        public void visitMethodEx(final MethodNode node) {
            spies.enter(getCurrentClassNode());
            super.visitMethodEx(node);
        }

        /**
         * Locals are collected before the block is judged, because a declaration always precedes the
         * statement that uses it and an earlier block's spies stay in scope for the later ones.
         */
        @Override
        public void visitBlock(final String label, final List<Statement> statements) {
            spies.collectLocals(statements);
            statements.forEach(this::judge);
        }

        @VisibleForTesting
        void judge(final Statement statement) {
            interactionOf(statement)
                    .filter(this::isUnanswered)
                    .flatMap(SpockInteraction::getTarget)
                    .flatMap(spies::spyNamedBy)
                    .ifPresent(spy -> addViolation(statement, messageFor(spy)));
        }

        /**
         * A counted interaction that is not {@code 0 *} and not the entry interaction. A stubbed one
         * has no cardinality here — it is read as written — and an {@code interaction { }} block has
         * none either, so neither is counted: "counted" already means "states no response".
         */
        @VisibleForTesting
        boolean isUnanswered(final SpockInteraction interaction) {
            return interaction.isCounted() && !interaction.isNeverCalled() && !interaction.isEntryCall();
        }

        @VisibleForTesting
        Optional<SpockInteraction> interactionOf(final Statement statement) {
            return statement instanceof ExpressionStatement
                    ? Optional.of(new SpockInteraction(((ExpressionStatement) statement).getExpression()))
                    : Optional.empty();
        }

        @VisibleForTesting
        String messageFor(final String spy) {
            return "State the response of this interaction on spy '" + spy
                    + "': without '>>' it calls the real method. Use '>> value', or '>> { callRealMethod() }' if that is meant.";
        }
    }
}
