package io.github.joke.lint.codenarc.rules.spock;

import static org.codenarc.rule.junit.SpockUtil.getSPOCK_LABELS;
import static org.codenarc.rule.junit.SpockUtil.isSpockFeatureMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codenarc.rule.AbstractAstVisitor;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Partitions a Spock feature method into its labelled blocks and hands each to the rule, so that the
 * three rules judging a <em>block</em> — is this {@code then:} terminated, does it hold a value
 * assertion, is this interaction in the right one — read one partition rather than three.
 *
 * <p>CodeNarc's own Spock rules carry a {@code currentLabel} field updated as expressions go by. That
 * style suits a rule judging statements one at a time; it cannot answer "what is the last statement
 * of this block", which is the question the terminator rule asks.
 *
 * <p>Groovy attaches a statement label to the first statement that follows it, so a labelled block is
 * a run of statements beginning at a labelled one and ending before the next. Partitioning is one
 * walk.
 *
 * <p>{@code and:} is absent from {@link org.codenarc.rule.junit.SpockUtil}'s label vocabulary — <em>"it
 * doesn't have any semantic impact"</em> — and that omission is what makes an {@code and:} continue
 * the block it follows rather than start one. A {@code then:} and its {@code and:} continuations
 * arrive here as a single block.
 *
 * <p>Statements before any label are the implicit setup region, reported under {@code setup} because
 * that is what Spock treats them as.
 *
 * <p>This class is {@code public} because the rules that extend it are separate classes. It is
 * <strong>not</strong> supported API: the artifact promises its rules and its rulesets, and makes no
 * compatibility statement about this class.
 */
public abstract class AbstractSpockBlockVisitor<R extends AbstractSpockRule> extends AbstractAstVisitor<R> {

    public static final String THEN_LABEL = "then";

    private static final String IMPLICIT_SETUP_LABEL = "setup";

    private static final List<String> SPOCK_LABELS = getSPOCK_LABELS();

    /**
     * A fixture method is not a feature method. {@code isSpockFeatureMethod} matches Spock's own
     * {@code SpecParser} — a method carrying at least one Spock statement label — so a {@code def
     * setup()} carrying none is skipped, and the cast below is safe because that test returns true
     * only for a method whose code is a {@link BlockStatement}.
     */
    @Override
    public void visitMethodEx(final MethodNode node) {
        if (isSpockFeatureMethod(node)) {
            visitBlocks(((BlockStatement) node.getCode()).getStatements());
        }
    }

    /**
     * The pending run is flushed when a labelled statement arrives, and once more at the end. The
     * labelled statement itself opens the run it labels, so only the very first flush can be empty —
     * the implicit setup region of a method that starts with a label.
     */
    @VisibleForTesting
    void visitBlocks(final List<Statement> statements) {
        var label = IMPLICIT_SETUP_LABEL;
        var block = new ArrayList<Statement>();
        for (final var statement : statements) {
            final var next = labelOf(statement);
            if (next.isPresent()) {
                visitBlock(label, block);
                label = next.get();
                block = new ArrayList<>();
            }
            block.add(statement);
        }
        visitBlock(label, block);
    }

    /**
     * A statement carries several labels when a labelled block is empty — {@code when:} immediately
     * followed by {@code then:} puts both on the statement after them. Groovy stacks them
     * innermost-first, so the label nearest the statement in the source is the <em>first</em> of the
     * list, and taking it is what stops an empty {@code when:} from swallowing the {@code then:}
     * block that follows it.
     *
     * <p>Labels outside Spock's vocabulary are skipped rather than ending the run, which is what makes
     * {@code and:} a continuation and leaves an ordinary loop label — {@code outer:} — alone.
     */
    @VisibleForTesting
    Optional<String> labelOf(final Statement statement) {
        final var labels = statement.getStatementLabels();
        return labels == null
                ? Optional.<String>empty()
                : labels.stream().filter(SPOCK_LABELS::contains).findFirst();
    }

    /**
     * One labelled run of top-level statements. The label is a Spock label or {@code setup} for the
     * implicit region; the statements are never empty except for that implicit region.
     */
    public abstract void visitBlock(String label, List<Statement> statements);
}
