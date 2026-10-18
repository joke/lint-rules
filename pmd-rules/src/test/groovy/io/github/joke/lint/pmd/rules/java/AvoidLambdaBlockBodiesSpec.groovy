package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.java.ast.ASTBlock
import net.sourceforge.pmd.lang.java.ast.ASTLambdaExpression
import net.sourceforge.pmd.reporting.RuleContext
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

/**
 * Each {@code @VisibleForTesting} method of {@link AvoidLambdaBlockBodies} is tested directly. The method
 * under test is the spy's real method; the siblings it calls are stubbed, so each feature sees one
 * decision.
 */
@Tag('unit')
@Subject(AvoidLambdaBlockBodies)
class AvoidLambdaBlockBodiesSpec extends Specification {

    ASTLambdaExpression lambda = Mock()
    RuleContext context = Mock()

    def 'visit delegates to the report decision and returns the data it was given'() {
        AvoidLambdaBlockBodies rule = Spy()

        when:
        def result = rule.visit(lambda, context)

        then:
        1 * rule.reportIfBlockBody(lambda, context) >> {}
        1 * rule._
        0 * _

        expect:
        result.is(context)
    }

    def 'reportIfBlockBody adds a violation when the body is an extractable block'() {
        AvoidLambdaBlockBodies rule = Spy()

        when:
        rule.reportIfBlockBody(lambda, context)

        then:
        1 * rule.hasExtractableBlockBody(lambda) >> true
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(lambda)
        0 * _
    }

    def 'reportIfBlockBody adds no violation otherwise'() {
        AvoidLambdaBlockBodies rule = Spy()

        when:
        rule.reportIfBlockBody(lambda, context)

        then:
        1 * rule.hasExtractableBlockBody(lambda) >> false
        1 * rule._
        0 * _
    }

    def 'hasExtractableBlockBody is false for an expression body, where there is no block'() {
        AvoidLambdaBlockBodies rule = new AvoidLambdaBlockBodies()

        when:
        def result = rule.hasExtractableBlockBody(lambda)

        then:
        1 * lambda.blockBody >> null
        0 * _

        expect:
        !result
    }

    def 'hasExtractableBlockBody is false for an empty block'() {
        AvoidLambdaBlockBodies rule = new AvoidLambdaBlockBodies()
        ASTBlock block = Mock()

        when:
        def result = rule.hasExtractableBlockBody(lambda)

        then:
        1 * lambda.blockBody >> block
        1 * block.empty >> true
        0 * _

        expect:
        !result
    }

    def 'hasExtractableBlockBody is true for a non-empty block'() {
        AvoidLambdaBlockBodies rule = new AvoidLambdaBlockBodies()
        ASTBlock block = Mock()

        when:
        def result = rule.hasExtractableBlockBody(lambda)

        then:
        1 * lambda.blockBody >> block
        1 * block.empty >> false
        0 * _

        expect:
        result
    }
}
