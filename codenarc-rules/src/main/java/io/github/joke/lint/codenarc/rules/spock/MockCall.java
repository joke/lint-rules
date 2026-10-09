package io.github.joke.lint.codenarc.rules.spock;

import java.util.Optional;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * One call to {@code Mock}, {@code Stub} or {@code Spy}, as {@link MockFactories} found it. The rules
 * that declare, scope or count doubles share this rather than each reading the call's arguments.
 */
final class MockCall {

    private static final String SPY = "Spy";
    private static final String CLASS_PROPERTY = "class";

    private final MethodCallExpression call;

    MockCall(final MethodCallExpression call) {
        this.call = call;
    }

    @VisibleForTesting
    boolean isSpy() {
        return SPY.equals(call.getMethodAsString());
    }

    /**
     * The name of the type the call is given as its type argument, as written, or nothing when it is given
     * none — {@code Mock()}, {@code Spy(constructorArgs: [...])} and {@code Spy(realInstance)} name no
     * type, the last because an instance is not one.
     *
     * <p>Named arguments are collected into a single map placed first, so the type is the first
     * argument that is not that map.
     */
    @VisibleForTesting
    Optional<String> typeArgumentName() {
        return ((TupleExpression) call.getArguments())
                .getExpressions().stream()
                        .filter(argument -> !(argument instanceof MapExpression))
                        .findFirst()
                        .flatMap(this::nameOf);
    }

    /**
     * CodeNarc has not resolved the source, so a class name arrives as a variable and a qualified or
     * {@code .class} one as a property chain; there is no class expression to read. A variable that
     * names an instance is told apart by its name differing from the declared type's.
     */
    @VisibleForTesting
    Optional<String> nameOf(final Expression expression) {
        if (expression instanceof VariableExpression) {
            return Optional.of(((VariableExpression) expression).getName());
        }
        if (expression instanceof PropertyExpression) {
            return nameOfProperty((PropertyExpression) expression);
        }
        return Optional.empty();
    }

    @VisibleForTesting
    Optional<String> nameOfProperty(final PropertyExpression property) {
        final var owner = nameOf(property.getObjectExpression());
        return CLASS_PROPERTY.equals(property.getPropertyAsString())
                ? owner
                : owner.map(name -> name + '.' + property.getPropertyAsString());
    }

    /**
     * Whether the call passes exactly the declared type as its type argument. Generics are ignored,
     * because the declared type's name does not carry them.
     *
     * <p>Either side may be qualified and the other not, since an import decides which is written, so
     * a name matches when it equals the other or ends in {@code .} and the other. Two qualified names
     * that differ — {@code java.util.List} and {@code java.awt.List} — are different types.
     */
    @VisibleForTesting
    boolean repeats(final ClassNode declared) {
        return typeArgumentName()
                .filter(name -> isSameType(name, declared.getName()))
                .isPresent();
    }

    @VisibleForTesting
    boolean isSameType(final String argument, final String declared) {
        return argument.equals(declared) || argument.endsWith('.' + declared) || declared.endsWith('.' + argument);
    }
}
