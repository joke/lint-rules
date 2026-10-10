package io.github.joke.lint.codenarc.rules.spock;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The doubles of one kind in scope for one feature method: the specification's fields plus the
 * method's locals that were initialised from a call the kind accepts. The spy rules ask for spies and
 * {@code AvoidCardinalityOnStub} asks for stubs; they share this rather than each deciding for itself
 * what a double is.
 *
 * <p>A double is recognised from a declaration whose initialiser is a {@code Mock}, {@code Stub} or
 * {@code Spy} call that the kind accepts, covering {@code Spy(Type)}, {@code Spy(constructorArgs: [ …
 * ])} and {@code Spy(realInstance)}. The declared type is not read: {@code
 * DeclareMockWithExplicitType} is separately selectable, and a consumer who has not adopted it still
 * has doubles. One arriving from a helper method, a base class or a parameter is invisible, and the
 * rules stay silent rather than infer it.
 *
 * <p>Fields are read from the class rather than accumulated as they are visited, because a field
 * declared below a feature method is still in scope inside it. {@link #enter} clears the locals so
 * that a double in one feature method is not a double in the next.
 */
final class DoubleScope {

    private final MockFactories factories = new MockFactories();
    private final Set<String> doubles = new HashSet<>();
    private final Predicate<MockCall> kind;

    DoubleScope(final Predicate<MockCall> kind) {
        this.kind = kind;
    }

    @VisibleForTesting
    void enter(final ClassNode classNode) {
        doubles.clear();
        classNode.getFields().forEach(this::collectField);
    }

    /**
     * Locals are collected from every block, including the one being judged, because a declaration
     * always precedes the block that verifies it.
     */
    @VisibleForTesting
    void collectLocals(final List<Statement> statements) {
        statements.forEach(this::collectLocal);
    }

    /** The double a target names, or nothing when it names none of the kind in scope. */
    @VisibleForTesting
    Optional<String> doubleNamedBy(final Expression target) {
        return receiverOf(target).filter(doubles::contains);
    }

    @VisibleForTesting
    void collectField(final FieldNode field) {
        if (factories.read(field.getInitialExpression()).filter(kind).isPresent()) {
            doubles.add(field.getName());
        }
    }

    @VisibleForTesting
    void collectLocal(final Statement statement) {
        if (statement instanceof ExpressionStatement) {
            collectDeclaration(((ExpressionStatement) statement).getExpression());
        }
    }

    @VisibleForTesting
    void collectDeclaration(final Expression expression) {
        if (expression instanceof DeclarationExpression
                && factories
                        .read(((DeclarationExpression) expression).getRightExpression())
                        .filter(kind)
                        .isPresent()) {
            nameOf(((DeclarationExpression) expression).getLeftExpression()).ifPresent(doubles::add);
        }
    }

    @VisibleForTesting
    Optional<String> receiverOf(final Expression target) {
        if (target instanceof MethodCallExpression) {
            return nameOf(((MethodCallExpression) target).getObjectExpression());
        }
        if (target instanceof PropertyExpression) {
            return nameOf(((PropertyExpression) target).getObjectExpression());
        }
        return nameOf(target);
    }

    /**
     * Empty for anything that is not a plain variable, which turns away the left side of a multiple
     * assignment and the implicit {@code this} of a bare call.
     */
    @VisibleForTesting
    Optional<String> nameOf(final Expression expression) {
        return expression instanceof VariableExpression
                ? Optional.of(((VariableExpression) expression).getName())
                : Optional.empty();
    }
}
