package io.github.joke.lint.codenarc.rules.spock;

import java.util.Set;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codenarc.rule.AbstractAstVisitor;
import org.codenarc.rule.AstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Reports a field that is {@code @Shared} or {@code static} and is initialised from {@code Mock},
 * {@code Stub} or {@code Spy}.
 *
 * <pre>{@code
 * @Shared CustomerRepository repository = Mock()     // reported
 * static CustomerRepository repository = Stub()      // reported
 * CustomerRepository repository = Mock()             // compliant
 * }</pre>
 *
 * <p>A double that outlives the feature method is outside the scope in which Spock verifies
 * interactions, so strict mocking cannot hold for it. Every double is created per feature method,
 * where its interactions are checked.
 *
 * <p>Only the declaration is read. {@code @Shared Foo foo} assigned in {@code setupSpec} has no
 * initialiser to inspect, and catching the assignment needs a write-tracking pass across methods;
 * the family under-reports rather than infers, and the gap is documented.
 *
 * <p>{@code @Shared} is matched by name as written, for the reason {@link AvoidUnrollAnnotationRule}
 * matches {@code @Unroll} that way: CodeNarc has no compile classpath to resolve it with.
 */
public class AvoidSharedOrStaticMockRule extends AbstractSpockRule {

    private static final String RULE_NAME = "AvoidSharedOrStaticMock";
    private static final int DEFAULT_PRIORITY = 2;

    public AvoidSharedOrStaticMockRule() {
        super(RULE_NAME, DEFAULT_PRIORITY);
    }

    @Override
    public Class<? extends AstVisitor> getAstVisitorClass() {
        return AvoidSharedOrStaticMockAstVisitor.class;
    }

    public static class AvoidSharedOrStaticMockAstVisitor extends AbstractAstVisitor<AvoidSharedOrStaticMockRule> {

        private final MockFactories factories = new MockFactories();

        private static final Set<String> SHARED = Set.of("Shared", "spock.lang.Shared");

        private static final String MESSAGE =
                "Create the double inside the feature method: a shared or static one lives outside the scope Spock verifies its interactions in.";

        @Override
        public void visitField(final FieldNode node) {
            if (isShared(node) && factories.read(node.getInitialExpression()).isPresent()) {
                addViolation(node, MESSAGE);
            }
        }

        @VisibleForTesting
        boolean isShared(final FieldNode node) {
            return node.isStatic() || node.getAnnotations().stream().anyMatch(this::isSharedAnnotation);
        }

        @VisibleForTesting
        boolean isSharedAnnotation(final AnnotationNode annotation) {
            return SHARED.contains(annotation.getClassNode().getName());
        }
    }
}
