package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import spock.lang.Specification
import spock.lang.Tag

import java.lang.reflect.Modifier

/**
 * Each method of {@link AvoidSharedOrStaticMockRule.AvoidSharedOrStaticMockAstVisitor} is tested directly. The
 * fields are built as nodes, so the modifier and annotation tests read the real node.
 */
@Tag('unit')
class AvoidSharedOrStaticMockAstVisitorSpec extends Specification {

    AvoidSharedOrStaticMockRule.AvoidSharedOrStaticMockAstVisitor visitor = Spy()

    def 'visitField reports a shared field initialised by a mock factory, including DeepMock'() {
        FieldNode field = field(Modifier.STATIC, call(factory))

        when:
        visitor.visitField(field)

        then:
        1 * visitor.isShared(field) >> true
        (factory == 'SpyStatic' ? 0 : 1) * visitor.addViolation(field,
                'Create the double inside the feature method: a shared or static one lives outside the scope Spock verifies its interactions in.') >> {}
        1 * visitor._
        0 * _

        where:
        factory << ['Mock', 'DeepMock', 'SpyStatic']
    }

    def 'visitField reports nothing for a field that is not shared, or is not initialised by a mock factory'() {
        FieldNode field = field(0, call('Mock'))

        when:
        visitor.visitField(field)

        then:
        1 * visitor.isShared(field) >> shared
        1 * visitor._
        0 * _

        where:
        shared << [false]
    }

    def 'visitField reports nothing for a shared field that is not a double'() {
        FieldNode field = field(Modifier.STATIC, new ConstantExpression(1))

        when:
        visitor.visitField(field)

        then:
        1 * visitor.isShared(field) >> true
        1 * visitor._
        0 * _
    }

    def 'isShared is true for a static field, and for a field annotated as shared'() {
        expect:
        visitor.isShared(field(modifiers, null, annotations)) == expected

        where:
        modifiers     | annotations               | expected
        Modifier.STATIC | []                      | true
        0             | ['Shared']                | true
        0             | ['spock.lang.Shared']     | true
        0             | ['Other']                 | false
    }

    def 'isSharedAnnotation is true for the simple and the qualified name of Shared'() {
        expect:
        visitor.isSharedAnnotation(new AnnotationNode(new ClassNode(name, 0, null))) == expected

        where:
        name                | expected
        'Shared'            | true
        'spock.lang.Shared' | true
        'Other'             | false
    }

    private static FieldNode field(int modifiers, org.codehaus.groovy.ast.expr.Expression initialiser,
                                   List<String> annotations = []) {
        def field = new FieldNode('repository', modifiers, new ClassNode('Repository', 0, null), null, initialiser)
        annotations.each { field.addAnnotation(new AnnotationNode(new ClassNode(it, 0, null))) }
        field
    }

    private static MethodCallExpression call(String name) {
        new MethodCallExpression(new VariableExpression('this'), name, new ArgumentListExpression())
    }
}
