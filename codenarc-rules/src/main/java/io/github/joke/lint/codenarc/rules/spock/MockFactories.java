package io.github.joke.lint.codenarc.rules.spock;

import java.util.Optional;
import java.util.Set;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

/**
 * Recognises a call to {@code Mock}, {@code Stub} or {@code Spy}. The rules that declare, scope or
 * count doubles share this rather than each restating which names are factories.
 *
 * <p>Matched by name only. The call arrives as an implicit-{@code this} invocation on the
 * specification, but nothing here depends on the receiver: resolving {@code Mock} to Spock's {@code
 * MockingApi} would need a compile classpath CodeNarc does not have, and the rules' class gate is what
 * keeps the bare name from matching outside a specification.
 *
 * <p>An instance, rather than static methods, so that a rule holds one and a test can stand one in.
 */
final class MockFactories {

    private static final Set<String> FACTORIES = Set.of("Mock", "Stub", "Spy");

    /** Nothing unless the expression is a call to one of the three factories. */
    @VisibleForTesting
    Optional<MockCall> read(final @Nullable Expression expression) {
        return expression instanceof MethodCallExpression && isFactory((MethodCallExpression) expression)
                ? Optional.of(new MockCall((MethodCallExpression) expression))
                : Optional.empty();
    }

    /**
     * Compared through the set rather than {@code contains}, because {@code getMethodAsString} is
     * null for a dynamically-named call and {@link Set#of} throws on a null lookup.
     */
    @VisibleForTesting
    boolean isFactory(final MethodCallExpression candidate) {
        return FACTORIES.stream().anyMatch(factory -> factory.equals(candidate.getMethodAsString()));
    }
}
