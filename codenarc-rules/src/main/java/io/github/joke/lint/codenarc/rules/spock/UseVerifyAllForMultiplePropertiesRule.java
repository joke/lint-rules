package io.github.joke.lint.codenarc.rules.spock;

import static java.util.stream.Collectors.toList;
import static org.codenarc.rule.junit.SpockUtil.isBooleanExpression;

import java.util.List;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports an interaction's constraint closure that asserts more than one property without {@code
 * verifyAll}.
 *
 * <pre>{@code
 * 1 * repository.save({ it.name == 'Ada' && it.region == 'eu' })   // reported — a chain
 * 1 * repository.save({ it.name == 'Ada'; it.region == 'eu' })     // reported — two conditions
 * 1 * auditLog.record({ it.action == 'CHECKOUT' })                 // compliant — one property
 *
 * 1 * repository.save({ Customer c ->                              // compliant
 *     verifyAll(c) {
 *         name == 'Ada'
 *         region == 'eu'
 *     }
 * })
 * }</pre>
 *
 * <p>{@code verifyAll} evaluates every condition and reports all failures at once. A chain stops at
 * the first, so a customer wrong in two fields reports one — and the second failure only appears
 * after the first is fixed and the specification is run again.
 *
 * <p><strong>A single property is exempt.</strong> The convention is explicit that the inline boolean
 * closure is enough for one property, and pushing {@code verifyAll} onto one condition would make the
 * common case wordier for no diagnostic gain — there is only one failure to report. A disjunction is
 * one condition too: {@code { it.region == 'eu' || it.region == 'uk' }} is a single question about a
 * single property, not two assertions that could each fail.
 *
 * <p>A condition is recognised by CodeNarc's own boolean-expression test, which is what keeps {@code
 * with}, {@code verifyAll} and {@code verifyEach} out of the reckoning without a carve-out: a closure
 * delegating to one of them holds a method call, and a call by any of those three names is not a
 * boolean expression under that test. So a wrapped closure holds no conditions of its own and is
 * silent however many it holds inside — the same three methods CodeNarc itself treats as carrying
 * implicit assertions.
 *
 * <p>The rule cannot collide with {@link RequireValidatedInteractionArgumentsRule}. That rule reports
 * a body of a single truthy constant, which is one condition; this one requires more than one. No
 * closure satisfies both predicates.
 *
 * <p>{@code expect:} blocks are out of scope. The convention names {@code verifyAll} as the right tool
 * for several properties of a returned value without making it a requirement, and the distinction it
 * rests on is not available to static analysis: several assertions about one value and assertions
 * about several different values have the same shape unless the rule knows what the expressions
 * denote. Only interaction constraint closures are examined, wherever the interaction stands — an
 * interaction moved out of {@code then:} to escape a sibling rule is still checked here.
 */
public class UseVerifyAllForMultiplePropertiesRule extends AbstractSpockRule {

    private static final String RULE_NAME = "UseVerifyAllForMultipleProperties";
    private static final int DEFAULT_PRIORITY = 2;

    public UseVerifyAllForMultiplePropertiesRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return UseVerifyAllForMultiplePropertiesAstVisitor.class;
    }

    public static class UseVerifyAllForMultiplePropertiesAstVisitor
            extends AbstractSpockBlockVisitor<UseVerifyAllForMultiplePropertiesRule> {

        private static final String CONJUNCTION = "&&";
        private static final int ONE_PROPERTY = 1;

        private static final String MESSAGE =
                "Assert the properties inside 'verifyAll', so a value wrong in two fields reports both failures rather than the first.";

        /**
         * Every block, not only {@code then:}. The label decides nothing here — what is judged is a
         * constraint closure, and an interaction carrying one is worth the same report wherever it was
         * written.
         */
        @Override
        public void visitBlock(final String label, final List<Statement> statements) {
            statements.forEach(this::reportConstraintClosures);
        }

        @VisibleForTesting
        void reportConstraintClosures(final Statement statement) {
            argumentsOf(statement).forEach(this::reportClosureArgument);
        }

        @VisibleForTesting
        List<Expression> argumentsOf(final Statement statement) {
            return statement instanceof ExpressionStatement
                    ? new SpockInteraction(((ExpressionStatement) statement).getExpression()).getArguments()
                    : List.of();
        }

        /** Once per closure, not once per condition: one closure is one thing to rewrite. */
        @VisibleForTesting
        void reportClosureArgument(final Expression argument) {
            if (argument instanceof ClosureExpression && assertsSeveralProperties((ClosureExpression) argument)) {
                addViolation(argument, MESSAGE);
            }
        }

        /**
         * The two reported shapes, and nothing between them: several conditions in the body, or one
         * condition that is a conjunction. Neither needs the operands counted — {@code a && b} and
         * {@code a && b && c} are the same violation with the same fix, and a count no report reads
         * would be arithmetic no test could observe.
         */
        @VisibleForTesting
        boolean assertsSeveralProperties(final ClosureExpression closure) {
            final var conditions = conditionsIn(closure);
            return conditions.size() > ONE_PROPERTY || conditions.stream().anyMatch(this::isConjunction);
        }

        @VisibleForTesting
        List<Expression> conditionsIn(final ClosureExpression closure) {
            return ((BlockStatement) closure.getCode())
                    .getStatements().stream()
                            .filter(this::isCondition)
                            .map(statement -> ((ExpressionStatement) statement).getExpression())
                            .collect(toList());
        }

        @VisibleForTesting
        boolean isCondition(final Statement statement) {
            return statement instanceof ExpressionStatement && isBooleanExpression((ExpressionStatement) statement);
        }

        /**
         * Only {@code &&} splits a condition. A disjunction is one question about one property, and
         * treating it as several would demand {@code verifyAll} for a closure with exactly one way to
         * fail.
         */
        @VisibleForTesting
        boolean isConjunction(final Expression expression) {
            return expression instanceof BinaryExpression
                    && CONJUNCTION.equals(
                            ((BinaryExpression) expression).getOperation().getText());
        }
    }
}
