package io.github.joke.lint.codenarc.rules.spock;

import static org.codenarc.rule.junit.SpockUtil.getClosureArgument;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

/**
 * One Spock interaction, read off one expression. The four rules in this package share this
 * classification rather than each deciding for itself what {@code *} and {@code >>} mean, so that no
 * two of them can disagree about whether a line is an interaction.
 *
 * <p>The forms recognised:
 *
 * <pre>{@code
 * 1 * mock.foo(x)             cardinality
 * mock.foo(x) >> value        stubbed return
 * mock.foo(x) >>> [a, b]      stubbed returns
 * 1 * mock.foo(x) >> value    both — '*' binds tighter, so the cardinality nests inside the stub
 * interaction { … }           an interaction block
 * }</pre>
 *
 * <p>A <strong>cardinality</strong> is a constant integer, a range or {@code _}; a
 * <strong>target</strong> is a method call, a property access or a bare variable. Both are needed:
 * without the cardinality test {@code total * price} would be an interaction, and without the target
 * test {@code 1 * (a + b)} would be one.
 *
 * <p>Recognition is by shape alone. Nothing here asks whether the target is a mock, a stub, a spy or
 * a real object — CodeNarc analyses source without a compile classpath, and the shape is what
 * distinguishes an interaction in the first place.
 *
 * <p>{@code >>} is also Groovy's right shift and its closure composition operator. At statement level
 * inside a Spock feature method the ambiguity is not worth modelling: a specification that
 * right-shifts an integer as a bare statement has a larger problem than a false report.
 *
 * <p>This class is {@code public} because the rules that read it are separate classes. It is
 * <strong>not</strong> supported API: the artifact promises its rules and its rulesets, and makes no
 * compatibility statement about this class.
 */
public final class SpockInteraction {

    private static final String CARDINALITY_OPERATOR = "*";
    private static final Set<String> STUB_OPERATORS = Set.of(">>", ">>>");
    private static final String INTERACTION_BLOCK = "interaction";
    private static final String WILDCARD = "_";
    private static final Integer NEVER = 0;

    /** Null unless the expression is a plain cardinality form. See {@link #cardinalityOf}. */
    @Nullable
    private final Expression cardinality;

    /** Null when the expression is not an interaction at all, which is what {@link #isPresent} reads. */
    @Nullable
    private final Expression target;

    public SpockInteraction(final Expression expression) {
        this.cardinality = cardinalityOf(expression);
        this.target = targetOf(expression);
    }

    public boolean isPresent() {
        return target != null;
    }

    /**
     * The expression the interaction constrains — a method call, a property access or a bare variable
     * — or nothing when the expression is not an interaction at all.
     *
     * <p>Read by {@link RequireSpyEntryInteractionRule}, the one rule that needs to know <em>which</em>
     * double an interaction names and whether it names one method or all of them. The others ask only
     * whether an interaction is present, whether it is the terminator, or what it passes.
     */
    public Optional<Expression> getTarget() {
        return Optional.ofNullable(target);
    }

    /**
     * {@code 0 * _} — the strict mocking terminator, and the one interaction that asserts about every
     * double at once rather than about a named collaborator.
     */
    public boolean isTerminator() {
        return isNever(cardinality) && isWildcard(target);
    }

    /**
     * The arguments of the target call, or nothing when the target is a property access or a bare
     * variable — {@code 1 * service._} and {@code 0 * _} name no arguments to constrain.
     *
     * <p>For an {@code interaction { }} block the target is the call itself, so this yields its
     * closure. No rule reads it there: the argument rule visits binary expressions, and the
     * interactions inside the closure are reached as binary expressions of their own.
     */
    public List<Expression> getArguments() {
        return target instanceof MethodCallExpression
                ? ((TupleExpression) ((MethodCallExpression) target).getArguments()).getExpressions()
                : List.of();
    }

    /**
     * Read off the expression as written, so a stubbed interaction carries no cardinality here even
     * though {@code *} binds tighter than {@code >>} and one is nested inside it. The only question
     * asked of a cardinality is {@link #isTerminator}, and {@code 0 * _ >> value} is not a terminator
     * anyone writes — reaching under the stub would add a branch no rule reads.
     */
    @VisibleForTesting
    @Nullable
    Expression cardinalityOf(final Expression expression) {
        return isCardinality(expression) ? ((BinaryExpression) expression).getLeftExpression() : null;
    }

    @VisibleForTesting
    @Nullable
    Expression targetOf(final Expression expression) {
        return isStubbedReturn(expression)
                ? stubbedTargetOf(((BinaryExpression) expression).getLeftExpression())
                : countedTargetOf(expression);
    }

    /**
     * Under a stub operator the left side is either a cardinality form or the target on its own,
     * which is what makes {@code mock.foo() >> value} an interaction while a bare {@code mock.foo()}
     * is an ordinary call.
     */
    @VisibleForTesting
    @Nullable
    Expression stubbedTargetOf(final Expression expression) {
        return isCardinality(expression) ? targetShape(rightOf(expression)) : targetShape(expression);
    }

    /**
     * Without a stub operator only a cardinality form or an {@code interaction { }} call is an
     * interaction. {@code service.warmUp()} is neither.
     */
    @VisibleForTesting
    @Nullable
    Expression countedTargetOf(final Expression expression) {
        return isCardinality(expression) ? targetShape(rightOf(expression)) : interactionBlock(expression);
    }

    @VisibleForTesting
    Expression rightOf(final Expression expression) {
        return ((BinaryExpression) expression).getRightExpression();
    }

    @VisibleForTesting
    boolean isStubbedReturn(final Expression expression) {
        return expression instanceof BinaryExpression
                && STUB_OPERATORS.contains(
                        ((BinaryExpression) expression).getOperation().getText());
    }

    /**
     * A declaration is a {@link BinaryExpression} too, carrying {@code =}, so {@code def total = 2 *
     * price} is turned away by the operator test before its right side is ever examined.
     */
    @VisibleForTesting
    boolean isCardinality(final Expression expression) {
        return expression instanceof BinaryExpression
                && CARDINALITY_OPERATOR.equals(
                        ((BinaryExpression) expression).getOperation().getText())
                && isCount(((BinaryExpression) expression).getLeftExpression());
    }

    @VisibleForTesting
    boolean isCount(final Expression expression) {
        return expression instanceof RangeExpression || isWildcard(expression) || isInteger(expression);
    }

    @VisibleForTesting
    boolean isInteger(final @Nullable Expression expression) {
        return expression instanceof ConstantExpression
                && ((ConstantExpression) expression).getValue() instanceof Integer;
    }

    @VisibleForTesting
    boolean isNever(final @Nullable Expression expression) {
        return expression instanceof ConstantExpression && NEVER.equals(((ConstantExpression) expression).getValue());
    }

    @VisibleForTesting
    boolean isWildcard(final @Nullable Expression expression) {
        return expression instanceof VariableExpression && WILDCARD.equals(((VariableExpression) expression).getName());
    }

    @VisibleForTesting
    @Nullable
    Expression targetShape(final Expression expression) {
        return expression instanceof MethodCallExpression
                        || expression instanceof PropertyExpression
                        || expression instanceof VariableExpression
                ? expression
                : null;
    }

    /**
     * Matched by name only, for the reason the mock factories are: the call arrives as an
     * implicit-{@code this} invocation, resolving it would need a compile classpath, and the rules'
     * class gate is what keeps the bare name from matching outside a specification.
     */
    @VisibleForTesting
    @Nullable
    Expression interactionBlock(final Expression expression) {
        return expression instanceof MethodCallExpression && isInteractionCall((MethodCallExpression) expression)
                ? expression
                : null;
    }

    @VisibleForTesting
    boolean isInteractionCall(final MethodCallExpression call) {
        return INTERACTION_BLOCK.equals(call.getMethodAsString()) && getClosureArgument(call) != null;
    }
}
