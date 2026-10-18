package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each method of {@link AvoidUnrollAnnotationRule.AvoidUnrollAnnotationAstVisitor} is tested directly. The
 * visitor is spied so that the method under test runs for real while the siblings it calls are stubbed.
 */
@Tag('unit')
class AvoidUnrollAnnotationAstVisitorSpec extends Specification {

    AvoidUnrollAnnotationRule.AvoidUnrollAnnotationAstVisitor visitor = Spy()

    def 'visitClassEx reports the unroll annotations of the class'() {
        ClassNode node = Mock()

        when:
        visitor.visitClassEx(node)

        then:
        1 * visitor.reportUnroll(node) >> {}
        1 * visitor._
        0 * _
    }

    def 'visitMethodEx reports the unroll annotations of the method'() {
        MethodNode node = Mock()

        when:
        visitor.visitMethodEx(node)

        then:
        1 * visitor.reportUnroll(node) >> {}
        1 * visitor._
        0 * _
    }

    def 'reportUnroll reports each annotation that is an unroll'() {
        AnnotatedNode node = Mock()
        AnnotationNode unroll = new AnnotationNode(new ClassNode('Unroll', 0, null))
        AnnotationNode other = new AnnotationNode(new ClassNode('Override', 0, null))

        when:
        visitor.reportUnroll(node)

        then:
        1 * node.annotations >> [unroll, other]
        1 * visitor.isUnroll(unroll) >> true
        1 * visitor.isUnroll(other) >> false
        1 * visitor.report(unroll) >> {}
        1 * visitor._
        0 * _
    }

    def 'isUnroll is true for the simple and the qualified name of the annotation'() {
        expect:
        visitor.isUnroll(new AnnotationNode(new ClassNode(name, 0, null))) == expected

        where:
        name                 | expected
        'Unroll'             | true
        'spock.lang.Unroll'  | true
        'NotUnroll'          | false
        'Override'           | false
    }

    def 'report adds a violation on the annotation'() {
        AnnotationNode annotation = new AnnotationNode(new ClassNode('Unroll', 0, null))

        when:
        visitor.report(annotation)

        then:
        1 * visitor.addViolation(annotation, 'Spock 2 unrolls by default, so @Unroll adds nothing.') >> {}
        1 * visitor._
        0 * _
    }
}
