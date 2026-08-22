package io.github.joke.lint.codenarc.rules.spock;

import static org.codenarc.rule.junit.SpockUtil.isSpockSpecification;

import org.codehaus.groovy.ast.ClassNode;
import org.codenarc.rule.AbstractAstVisitorRule;
import org.codenarc.rule.AstVisitor;
import org.jspecify.annotations.Nullable;

/**
 * The CodeNarc rule contract and the Spock specification gate, carried once for every rule this
 * artifact ships. A concrete rule declares its defaults and its visitor and nothing else.
 *
 * <p>{@code PMD.DataClass} is suppressed because CodeNarc's rule contract mandates the shape the
 * rule reports: {@link org.codenarc.rule.AbstractRule} declares {@code name} and {@code priority} as
 * abstract read-write properties, because a ruleset configures a rule by setting them, and the
 * specification gate adds the two properties every stock Spock rule exposes. That is eight accessors
 * a rule class cannot decline to have. Carrying them here states the suppression once for the whole
 * artifact rather than once per rule, and answers PMD without excluding {@code DataClass} from the
 * ruleset this project publishes to consumers.
 *
 * <p>This class is {@code public} because CodeNarc instantiates rule classes reflectively and Java
 * visibility offers no narrower option. It is <strong>not</strong> supported API: the artifact
 * promises its rules and its rulesets, and makes no compatibility statement about this class.
 */
@SuppressWarnings("PMD.DataClass")
public abstract class AbstractSpockRule extends AbstractAstVisitorRule {

    private static final String DEFAULT_SPECIFICATION_SUPERCLASS_NAMES = "*Specification";

    private String name;
    private int priority;
    private String specificationSuperclassNames = DEFAULT_SPECIFICATION_SUPERCLASS_NAMES;

    /** Unset by default, so the superclass pattern alone decides unless a consumer says otherwise. */
    @Nullable
    private String specificationClassNames;

    /**
     * Concrete rules keep a public no-argument constructor, because CodeNarc's {@code
     * RuleSetBuilder} instantiates a rule class by calling {@code newInstance()}. Their defaults
     * arrive here.
     */
    protected AbstractSpockRule(final String name, final int priority) {
        super();
        this.name = name;
        this.priority = priority;
    }

    /**
     * Abstract rather than inherited, so that a rule cannot forget its visitor and fail at analysis
     * time with CodeNarc's "astVisitorClass property must not be null".
     */
    @Override
    public abstract Class<? extends AstVisitor> getAstVisitorClass();

    /**
     * The gate, applied where CodeNarc already asks whether a rule applies to a class, so that no
     * visitor carries it and a rule that forgets it cannot exist.
     *
     * <p>{@code super} is called first so that CodeNarc's own {@code applyToClassNames} and {@code
     * doNotApplyToClassNames} keep working — they are part of every rule's configuration surface and
     * silently dropping them would be a regression against the stock rules.
     */
    @Override
    protected boolean shouldApplyThisRuleTo(final ClassNode classNode) {
        return super.shouldApplyThisRuleTo(classNode)
                && isSpockSpecification(classNode, specificationSuperclassNames, specificationClassNames);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public String getSpecificationSuperclassNames() {
        return specificationSuperclassNames;
    }

    public void setSpecificationSuperclassNames(final String specificationSuperclassNames) {
        this.specificationSuperclassNames = specificationSuperclassNames;
    }

    @Nullable
    public String getSpecificationClassNames() {
        return specificationClassNames;
    }

    public void setSpecificationClassNames(@Nullable final String specificationClassNames) {
        this.specificationClassNames = specificationClassNames;
    }
}
