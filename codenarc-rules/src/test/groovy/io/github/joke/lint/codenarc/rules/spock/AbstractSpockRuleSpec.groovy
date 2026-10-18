package io.github.joke.lint.codenarc.rules.spock

import org.codehaus.groovy.ast.ClassNode
import org.codenarc.rule.junit.SpockUtil
import spock.lang.Specification
import spock.lang.Tag

/**
 * {@link AbstractSpockRule} carries the rule contract and the Spock specification gate for every rule. It
 * is exercised through a concrete rule, the smallest one, since the base class is abstract.
 */
@Tag('unit')
class AbstractSpockRuleSpec extends Specification {

    AvoidUnrollAnnotationRule rule = new AvoidUnrollAnnotationRule()
    ClassNode classNode = new ClassNode('ExampleSpec', 0, null)

    def 'the name and priority are the defaults the rule was constructed with, and are settable'() {
        expect:
        rule.name == 'AvoidUnrollAnnotation'
        rule.priority == 2

        when:
        rule.name = 'Renamed'
        rule.priority = 1

        then:
        0 * _

        expect:
        rule.name == 'Renamed'
        rule.priority == 1
    }

    def 'the specification superclass pattern defaults to any Specification and is settable'() {
        expect:
        rule.specificationSuperclassNames == '*Specification'

        when:
        rule.specificationSuperclassNames = 'Base'

        then:
        0 * _

        expect:
        rule.specificationSuperclassNames == 'Base'
    }

    def 'the specification class names are unset by default and are settable'() {
        expect:
        rule.specificationClassNames == null

        when:
        rule.specificationClassNames = 'ExampleSpec'

        then:
        0 * _

        expect:
        rule.specificationClassNames == 'ExampleSpec'
    }

    def 'a class is applied to only when the specification gate accepts it'() {
        SpyStatic(SpockUtil)

        when:
        def applies = rule.shouldApplyThisRuleTo(classNode)

        then:
        1 * SpockUtil.isSpockSpecification(classNode, '*Specification', null) >> gate
        0 * _

        expect:
        applies == gate

        where:
        gate << [true, false]
    }

    def 'the gate is passed the configured superclass and class names'() {
        SpyStatic(SpockUtil)
        rule.specificationSuperclassNames = 'Base'
        rule.specificationClassNames = 'ExampleSpec'

        when:
        rule.shouldApplyThisRuleTo(classNode)

        then:
        1 * SpockUtil.isSpockSpecification(classNode, 'Base', 'ExampleSpec') >> true
        0 * _
    }

    def 'the superclass applicability is consulted before the gate, so a class it excludes never reaches the gate'() {
        SpyStatic(SpockUtil)
        rule.applyToClassNames = 'Other'

        expect:
        !rule.shouldApplyThisRuleTo(classNode)
    }
}
