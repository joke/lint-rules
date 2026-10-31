package io.github.joke.lint.pmd.rules.java;

import static net.sourceforge.pmd.properties.NumericConstraints.inRange;
import static net.sourceforge.pmd.properties.PropertyFactory.intProperty;

import java.util.List;
import net.sourceforge.pmd.lang.java.ast.ASTArrayInitializer;
import net.sourceforge.pmd.lang.java.ast.ASTExpression;
import net.sourceforge.pmd.lang.java.ast.ASTForeachStatement;
import net.sourceforge.pmd.lang.java.ast.ASTLambdaExpression;
import net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodReference;
import net.sourceforge.pmd.lang.java.ast.ASTNullLiteral;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclarator;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRulechainRule;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

/**
 * Reports local variable declarations written with an explicit type where {@code var} would compile
 * and preserve the declared type.
 *
 * <p>Targeting {@link ASTLocalVariableDeclaration} rather than the variable ids is what keeps the
 * rule honest about scope: fields sit under an {@code ASTFieldDeclaration}, and method, constructor,
 * catch and lambda parameters have no local variable declaration ancestor at all, so none of them is
 * ever visited.
 */
public class UseVarForLocalVariables extends AbstractJavaRulechainRule {

    private static final int ONE_DECLARATOR = 1;

    /** The first Java language level that has {@code var}. */
    private static final int VAR_SINCE = 10;

    /** The property value meaning the consumer has not stated a target, so PMD's own gate alone applies. */
    private static final int UNSET = 0;

    /** Initializers that carry no type of their own for {@code var} to infer. */
    private static final List<Class<? extends ASTExpression>> UNINFERABLE_INITIALIZERS = List.of(
            ASTNullLiteral.class, ASTArrayInitializer.class, ASTLambdaExpression.class, ASTMethodReference.class);

    /**
     * The Java level the consumer compiles for, stated in its ruleset. PMD 7 under Gradle analyses at its own
     * default language version, so this is the only way the rule learns that a Java 8 codebase cannot use
     * {@code var}.
     */
    @VisibleForTesting
    static final PropertyDescriptor<Integer> TARGET_JAVA_VERSION = intProperty("targetJavaVersion")
            .desc("Java language level of the code being analysed, for example 8. Below 10 the rule reports nothing. "
                    + "0 leaves the decision to PMD's minimumLanguageVersion gate alone.")
            .require(inRange(UNSET, 99))
            .defaultValue(UNSET)
            .build();

    /**
     * {@code definePropertyDescriptor} is PMD's registration hook for rule properties, not a method a subclass
     * could override to change behaviour here, so calling it from the constructor is safe.
     */
    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    public UseVarForLocalVariables() {
        super(ASTLocalVariableDeclaration.class);
        definePropertyDescriptor(TARGET_JAVA_VERSION);
    }

    /**
     * Holds no logic of its own: every decision lives in {@link #isRewritableAsVar}, which a test
     * stubs on a spy to reach both branches here without parsing anything.
     */
    @Override
    public Object visit(final ASTLocalVariableDeclaration node, final Object data) {
        reportIfRewritableAsVar(node, data);
        return data;
    }

    @VisibleForTesting
    void reportIfRewritableAsVar(final ASTLocalVariableDeclaration node, final Object data) {
        if (targetSupportsVar() && isRewritableAsVar(node)) {
            asCtx(data).addViolation(node);
        }
    }

    /** True when the consumer's target is unset or is a level that has {@code var}. */
    @VisibleForTesting
    boolean targetSupportsVar() {
        final var target = getProperty(TARGET_JAVA_VERSION);
        return target == UNSET || target >= VAR_SINCE;
    }

    @VisibleForTesting
    boolean isRewritableAsVar(final ASTLocalVariableDeclaration node) {
        if (node.isTypeInferred()) {
            return false;
        }
        // `int a = 1, b = 2;` cannot be rewritten: var forbids multiple declarators.
        final var declarators = node.children(ASTVariableDeclarator.class).toList();
        return declarators.size() == ONE_DECLARATOR && declaresAnInferableType(node, declarators.get(0));
    }

    @VisibleForTesting
    boolean declaresAnInferableType(final ASTLocalVariableDeclaration node, final ASTVariableDeclarator declarator) {
        // The variable of an enhanced for loop has no initializer of its own — the loop assigns it,
        // and `for (var s : names)` is legal — so it is decided before the initializer is examined.
        return node.getParent() instanceof ASTForeachStatement || isInferableInitializer(declarator.getInitializer());
    }

    /**
     * An initializer lets {@code var} infer the declared type unless it is absent, is the
     * {@code null} literal, or is one of the poly expressions that has no type without a target
     * type to give it one.
     */
    @VisibleForTesting
    boolean isInferableInitializer(final @Nullable ASTExpression initializer) {
        return initializer != null && UNINFERABLE_INITIALIZERS.stream().noneMatch(type -> type.isInstance(initializer));
    }
}
