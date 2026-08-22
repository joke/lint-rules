package io.github.joke.lint.codenarc.rules.spock;

import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codenarc.rule.AbstractAstVisitor;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports {@code @Unroll} on a specification class or a feature method.
 *
 * <p>Spock 2 unrolls every data-driven feature by default, so the annotation adds nothing to what
 * the runner already does. Left in place it reads as though it were switching a behaviour on, which
 * sends a reader looking for the un-annotated features that supposedly behave differently.
 *
 * <p>The rule contract accessors and the specification gate live on {@link AbstractSpockRule}. The
 * gate narrows this rule: {@code @Unroll} outside a Spock specification is no longer reported.
 * Nothing else annotates with {@code Unroll}, so the narrowing costs nothing and buys one answer to
 * "when does a rule in this artifact apply" instead of two.
 */
public class AvoidUnrollAnnotationRule extends AbstractSpockRule {

    private static final String RULE_NAME = "AvoidUnrollAnnotation";
    private static final int DEFAULT_PRIORITY = 2;

    public AvoidUnrollAnnotationRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return AvoidUnrollAnnotationAstVisitor.class;
    }

    /**
     * Neither visit method calls {@code super}. {@code visitClassEx} and {@code visitMethodEx} are
     * empty hooks on {@link AbstractAstVisitor} — the traversal itself is driven by the {@code
     * final} {@code visitClass} and {@code visitMethod} that call them — so a {@code super} call
     * would be a statement no test could ever distinguish the absence of.
     */
    public static class AvoidUnrollAnnotationAstVisitor extends AbstractAstVisitor<AvoidUnrollAnnotationRule> {

        private static final String UNROLL = "Unroll";
        private static final String UNROLL_QUALIFIED = "spock.lang.Unroll";
        private static final String MESSAGE = "Spock 2 unrolls by default, so @Unroll adds nothing.";

        @Override
        public void visitClassEx(final ClassNode node) {
            reportUnroll(node);
        }

        @Override
        public void visitMethodEx(final MethodNode node) {
            reportUnroll(node);
        }

        @VisibleForTesting
        void reportUnroll(final AnnotatedNode node) {
            node.getAnnotations().stream().filter(this::isUnroll).forEach(this::report);
        }

        /**
         * Matches on the name as written, because resolving the annotation would need a compile
         * classpath and CodeNarc analyses source without one. Both the simple and the qualified form
         * are accepted since either compiles.
         */
        @VisibleForTesting
        boolean isUnroll(final AnnotationNode annotation) {
            final var declared = annotation.getClassNode().getName();
            return UNROLL.equals(declared) || UNROLL_QUALIFIED.equals(declared);
        }

        @VisibleForTesting
        void report(final AnnotationNode annotation) {
            addViolation(annotation, MESSAGE);
        }
    }
}
