package io.github.joke.lint.codenarc.rules.spock;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a {@code then:} block that declares interactions on a {@code Spy} without also declaring
 * {@code 1 * spy._}.
 *
 * <pre>{@code
 * OrderService service = Spy(constructorArgs: [repository])
 *
 * when:
 * service.placeOrder(order)
 *
 * then:
 * 1 * service.validate(order)     // a sibling, stubbed on the spy
 * 1 * repository.persist(order)
 * 1 * service._                   // required: accounts for the placeOrder() entry call
 * 0 * _
 * }</pre>
 *
 * <p>A spy's own methods count as interactions under strict mocking. The method under test is itself
 * invoked once, by {@code when:}, and that entry call is an interaction on the spy like any other —
 * so without {@code 1 * spy._} to account for it, the {@code 0 * _} terminator fails on the very call
 * the feature method exists to make.
 *
 * <p><strong>This is the only rule in the family whose fix is to add a line.</strong> Every other one
 * reports something present that should be moved or removed.
 *
 * <p>The entry interaction is required only where a <em>specific</em> interaction on the spy exists. A
 * block that constrains nothing on the spy is not about to fail its terminator on the entry call, so
 * the line would assert nothing there. {@code 1 * spy._} is itself an interaction on the spy, and it
 * does not count as the specific interaction that triggers the requirement — counting the remedy as
 * its own trigger would satisfy the rule in exactly the blocks that never needed it.
 *
 * <p><strong>Position is checked, and this is the one rule that checks it.</strong> Spock matches a
 * declared interaction against a call in declaration order, and {@code 1 * spy._} matches every method
 * on the spy. Declared first it absorbs the sibling calls the specific interactions were written to
 * verify, and those then fail their own counts — the specification still runs, and fails somewhere
 * other than where the mistake is. Ordering earns the exception because the wrong order is silently
 * wrong.
 *
 * <p>A spy is recognised from a declaration whose initialiser is a {@code Spy} call, as a local in the
 * feature method or as a field on the specification. One arriving from a helper method, a base class
 * or a parameter is invisible and the rule stays silent: inferring would demand {@code 1 * x._} for a
 * variable that is not a spy, and the only way to satisfy that demand is to add a line that breaks the
 * specification.
 */
public class RequireSpyEntryInteractionRule extends AbstractSpockRule {

    private static final String RULE_NAME = "RequireSpyEntryInteraction";
    private static final int DEFAULT_PRIORITY = 2;

    public RequireSpyEntryInteractionRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return RequireSpyEntryInteractionAstVisitor.class;
    }

    public static class RequireSpyEntryInteractionAstVisitor
            extends AbstractSpockBlockVisitor<RequireSpyEntryInteractionRule> {

        private final SpyScope spies = new SpyScope();

        /**
         * Entered for every method so that the spy fields are those of the current class and the
         * locals are those of the current feature method.
         */
        @Override
        public void visitMethodEx(final MethodNode node) {
            spies.enter(getCurrentClassNode());
            super.visitMethodEx(node);
        }

        /**
         * A {@code then:} run is judged after its own statements have been read, which costs
         * nothing: a spy declared inside {@code then:} has no interactions above it.
         */
        @Override
        public void visitBlock(final String label, final List<Statement> statements) {
            spies.collectLocals(statements);
            if (THEN_LABEL.equals(label)) {
                judgeRun(statements);
            }
        }

        /**
         * Each spy is judged over the whole run, so two spies are independent and two runs in one
         * feature method neither share an entry interaction nor inherit each other's order.
         */
        @VisibleForTesting
        void judgeRun(final List<Statement> statements) {
            spiesNamedIn(statements).forEach(spy -> judgeSpy(spy, statements));
        }

        /** In first-appearance order, so two spies report in the order the block names them. */
        @VisibleForTesting
        Set<String> spiesNamedIn(final List<Statement> statements) {
            return statements.stream()
                    .map(this::spyNamedBy)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toCollection(LinkedHashSet::new));
        }

        /**
         * The whole requirement, in one question: the last interaction on the spy must be {@code 1 *
         * spy._}. Anything else last means either that the entry interaction is missing, or that a
         * specific interaction was declared behind it — and the two are the same mistake seen from
         * either side, so one test separates them.
         *
         * <p>The list is never empty: a spy is judged only because the run named it.
         */
        @VisibleForTesting
        void judgeSpy(final String spy, final List<Statement> statements) {
            final var interactions = interactionsOn(spy, statements);
            final var last = interactions.get(interactions.size() - 1);
            if (!isEntryInteraction(last)) {
                reportSpy(spy, interactions, last);
            }
        }

        /**
         * An entry interaction somewhere above is a misplaced one; none at all is a missing one.
         * Reported on the entry interaction in the first case and on the last specific interaction in
         * the second, so each violation points at the line to move or the line to write after.
         */
        @VisibleForTesting
        void reportSpy(final String spy, final List<Statement> interactions, final Statement last) {
            interactions.stream()
                    .filter(this::isEntryInteraction)
                    .findFirst()
                    .ifPresentOrElse(
                            entry -> addViolation(
                                    entry,
                                    "Declare '1 * " + spy
                                            + "._' after the specific interactions on the spy: declared first it absorbs the calls they verify."),
                            () -> addViolation(
                                    last,
                                    "Add '1 * " + spy
                                            + "._' here, or '0 * _' fails on the call 'when:' made on the spy."));
        }

        @VisibleForTesting
        List<Statement> interactionsOn(final String spy, final List<Statement> statements) {
            return statements.stream()
                    .filter(statement ->
                            spyNamedBy(statement).filter(spy::equals).isPresent())
                    .collect(toList());
        }

        /** The spy an interaction names, or nothing when the statement names no spy in scope. */
        @VisibleForTesting
        Optional<String> spyNamedBy(final Statement statement) {
            return targetOf(statement).flatMap(spies::spyNamedBy);
        }

        @VisibleForTesting
        Optional<Expression> targetOf(final Statement statement) {
            return expressionOf(statement).map(SpockInteraction::new).flatMap(SpockInteraction::getTarget);
        }

        /**
         * The cardinality is not read: the convention writes {@code 1 *}, a spy entered twice writes
         * {@code 2 *}, and reading the count would report the second.
         */
        @VisibleForTesting
        boolean isEntryInteraction(final Statement statement) {
            return expressionOf(statement)
                    .map(SpockInteraction::new)
                    .filter(SpockInteraction::isEntryCall)
                    .isPresent();
        }

        @VisibleForTesting
        Optional<Expression> expressionOf(final Statement statement) {
            return statement instanceof ExpressionStatement
                    ? Optional.of(((ExpressionStatement) statement).getExpression())
                    : Optional.empty();
        }
    }
}
