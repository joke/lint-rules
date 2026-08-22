package io.github.joke.lint.codenarc.rules.spock;

import java.util.Optional;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codenarc.rule.AbstractAstVisitor;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports an interaction argument that places no constraint on the value passed.
 *
 * <pre>{@code
 * 1 * repository.save(_)                  // reported
 * 1 * repository.save(_ as Customer)      // reported
 * 1 * repository.save(*_)                 // reported
 * 1 * repository.save({ true })           // reported
 * 1 * repository.save(expectedCustomer)   // compliant
 * 1 * repository.save({ it.id == 'c-1' }) // compliant
 * }</pre>
 *
 * <p>A mocked interaction is a contract about what the subject passes its collaborator. An argument
 * that matches anything lets a wrong value through and leaves the contract asserting only that a call
 * happened, which is the weaker half.
 *
 * <p>{@code _ as Type} is reported too. It carries more information than a bare {@code _}, which makes
 * it the closest call in the set, but it still accepts every instance of that type and the rule's
 * subject is whether the <em>argument</em> was the right one. A project that disagrees excludes the
 * rule.
 *
 * <p>For the closure form the parameter list is irrelevant: {@code { true }}, {@code { _ -> true }} and
 * {@code { it -> true }} differ only in a parameter the body never reads, so only the body is
 * examined — one statement whose expression is a constant truthy under Groovy truth. A closure that is
 * truthy without being constant, such as {@code { it }}, is <strong>not</strong> reported; detecting
 * it would begin a general truthiness analysis this rule declines to start.
 *
 * <p>Only argument positions are examined. {@code _} in target position is mandated by this rule's own
 * family — {@code 0 * _} by {@link RequireStrictMockingTerminatorRule}, {@code 1 * subject._} by the
 * spy convention — and a rule reading {@code _} wherever it appeared would report what its siblings
 * require.
 *
 * <p>Reported wherever the interaction appears, including a field initialiser and a {@code Mock() { …
 * }} closure, so that an interaction moved out of a {@code then:} block to escape a sibling rule is
 * still checked here.
 */
public class RequireValidatedInteractionArgumentsRule extends AbstractSpockRule {

    private static final String RULE_NAME = "RequireValidatedInteractionArguments";
    private static final int DEFAULT_PRIORITY = 2;

    public RequireValidatedInteractionArgumentsRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return RequireValidatedInteractionArgumentsAstVisitor.class;
    }

    /**
     * An expression visitor rather than a block visitor: this is the one rule of the four that judges
     * a statement wherever it stands, so it has no use for the feature method's partition.
     */
    public static class RequireValidatedInteractionArgumentsAstVisitor
            extends AbstractAstVisitor<RequireValidatedInteractionArgumentsRule> {

        private static final String WILDCARD = "_";

        private static final String MESSAGE =
                "Pass the value the interaction expects: an argument that matches anything asserts only that a call happened.";

        @Override
        public void visitBinaryExpression(final BinaryExpression expression) {
            reportUnconstrainedArguments(expression);
            super.visitBinaryExpression(expression);
        }

        @VisibleForTesting
        void reportUnconstrainedArguments(final BinaryExpression expression) {
            new SpockInteraction(expression).getArguments().forEach(this::reportUnconstrained);
        }

        /**
         * {@code isFirstVisit} on the argument, because the same argument node is reached twice by two
         * different routes: a combined {@code 1 * mock.foo(_) >> value} is an interaction at the stub
         * and again at the cardinality nested inside it, and a non-private field is traversed once as
         * a field and once as a property.
         */
        @VisibleForTesting
        void reportUnconstrained(final Expression argument) {
            if (isFirstVisit(argument) && matchesAnything(argument)) {
                addViolation(argument, MESSAGE);
            }
        }

        @VisibleForTesting
        boolean matchesAnything(final Expression argument) {
            return isWildcard(unwrapped(argument)) || isAlwaysTrue(argument);
        }

        /** {@code _ as Type} is a cast and {@code *_} a spread; both wrap the wildcard they narrow. */
        @VisibleForTesting
        Expression unwrapped(final Expression argument) {
            if (argument instanceof CastExpression) {
                return ((CastExpression) argument).getExpression();
            }
            if (argument instanceof SpreadExpression) {
                return ((SpreadExpression) argument).getExpression();
            }
            return argument;
        }

        @VisibleForTesting
        boolean isWildcard(final Expression expression) {
            return expression instanceof VariableExpression
                    && WILDCARD.equals(((VariableExpression) expression).getName());
        }

        @VisibleForTesting
        boolean isAlwaysTrue(final Expression argument) {
            return argument instanceof ClosureExpression
                    && constantBodyOf((ClosureExpression) argument)
                            .filter(DefaultTypeTransformation::castToBoolean)
                            .isPresent();
        }

        /**
         * Groovy truth rather than a test for {@code true}, so {@code { 1 }} is reported and {@code { 0
         * }} is not — a closure constrains nothing exactly when its constant body is truthy.
         */
        @VisibleForTesting
        Optional<Object> constantBodyOf(final ClosureExpression closure) {
            return onlyStatementOf(closure)
                    .filter(statement -> statement instanceof ExpressionStatement)
                    .map(statement -> ((ExpressionStatement) statement).getExpression())
                    .filter(expression -> expression instanceof ConstantExpression)
                    .map(expression -> ((ConstantExpression) expression).getValue());
        }

        @VisibleForTesting
        Optional<Statement> onlyStatementOf(final ClosureExpression closure) {
            final var statements = ((BlockStatement) closure.getCode()).getStatements();
            return statements.size() == 1 ? Optional.of(statements.get(0)) : Optional.empty();
        }
    }
}
