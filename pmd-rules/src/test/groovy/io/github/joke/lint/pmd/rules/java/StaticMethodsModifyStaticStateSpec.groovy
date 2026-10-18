package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.ast.NodeStream
import net.sourceforge.pmd.lang.java.ast.ASTAssignableExpr
import net.sourceforge.pmd.lang.java.ast.ASTAssignableExpr.ASTNamedReferenceExpr
import net.sourceforge.pmd.lang.java.ast.ASTClassType
import net.sourceforge.pmd.lang.java.ast.ASTConstructorDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTType
import net.sourceforge.pmd.lang.java.ast.ASTTypeDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTVoidType
import net.sourceforge.pmd.lang.java.ast.Annotatable
import net.sourceforge.pmd.lang.java.ast.ModifierOwner.Visibility
import net.sourceforge.pmd.lang.java.symbols.JFieldSymbol
import net.sourceforge.pmd.lang.java.symbols.JVariableSymbol
import net.sourceforge.pmd.reporting.RuleContext
import spock.lang.Specification
import spock.lang.Tag

import java.lang.reflect.Modifier
import java.util.function.Predicate

/**
 * Each {@code @VisibleForTesting} method of {@link StaticMethodsModifyStaticState} is tested directly. The
 * method under test is the spy's real method; the siblings it calls are stubbed, so each feature sees one
 * decision.
 */
@Tag('unit')
class StaticMethodsModifyStaticStateSpec extends Specification {

    ASTMethodDeclaration method = Mock()
    RuleContext context = Mock()

    def 'visit delegates to the report decision and returns the data it was given'() {
        StaticMethodsModifyStaticState rule = Spy()

        when:
        def result = rule.visit(method, context)

        then:
        1 * rule.reportIfUnjustifiedStatic(method, context) >> {}
        1 * rule._
        0 * _

        expect:
        result.is(context)
    }

    def 'reportIfUnjustifiedStatic adds a violation for an unjustified static'() {
        StaticMethodsModifyStaticState rule = Spy()

        when:
        rule.reportIfUnjustifiedStatic(method, context)

        then:
        1 * rule.isUnjustifiedStatic(method) >> true
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(method)
        0 * _
    }

    def 'reportIfUnjustifiedStatic adds no violation otherwise'() {
        StaticMethodsModifyStaticState rule = Spy()

        when:
        rule.reportIfUnjustifiedStatic(method, context)

        then:
        1 * rule.isUnjustifiedStatic(method) >> false
        1 * rule._
        0 * _
    }

    def 'isUnjustifiedStatic needs a static method that is not justified'() {
        StaticMethodsModifyStaticState rule = Spy()

        when:
        def result = rule.isUnjustifiedStatic(method)

        then:
        1 * method.static >> isStatic
        (isStatic ? 1 : 0) * rule.isJustified(method) >> justified
        1 * rule._
        0 * _

        expect:
        result == expected

        where:
        isStatic | justified | expected
        false    | false     | false
        true     | true      | false
        true     | false     | true
    }

    def 'isJustified is true when any one of its four exemptions holds'() {
        StaticMethodsModifyStaticState rule = Spy()

        when:
        def result = rule.isJustified(method)

        then:
        1 * rule.isRequiredStaticByPlatform(method) >> platform
        (platform ? 0 : 1) * rule.declaredInUtilityClass(method) >> utility
        (platform || utility ? 0 : 1) * rule.isNamedConstructor(method) >> namedConstructor
        (platform || utility || namedConstructor ? 0 : 1) * rule.writesPrivateStaticField(method) >> writesPrivate
        1 * rule._
        0 * _

        expect:
        result == expected

        where:
        platform | utility | namedConstructor | writesPrivate | expected
        true     | false   | false            | false         | true
        false    | true    | false            | false         | true
        false    | false   | true             | false         | true
        false    | false   | false            | true          | true
        false    | false   | false            | false         | false
    }

    def 'isRequiredStaticByPlatform is true for a main method, without looking at annotations'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()

        when:
        def result = rule.isRequiredStaticByPlatform(method)

        then:
        1 * method.mainMethod >> true
        0 * _

        expect:
        result
    }

    def 'isRequiredStaticByPlatform is decided by the framework lifecycle annotations otherwise'() {
        StaticMethodsModifyStaticState rule = Spy()

        when:
        def result = rule.isRequiredStaticByPlatform(method)

        then:
        1 * method.mainMethod >> false
        1 * rule.hasAnyAnnotation(method, ['BeforeAll', 'AfterAll'] as Set) >> hasAnnotation
        1 * rule._
        0 * _

        expect:
        result == hasAnnotation

        where:
        hasAnnotation << [true, false]
    }

    def 'isNamedConstructor is false for a result type that is not a class type'() {
        StaticMethodsModifyStaticState rule = Spy()
        ASTType resultType = Mock(ASTVoidType)

        when:
        def result = rule.isNamedConstructor(method)

        then:
        1 * method.resultTypeNode >> resultType
        1 * rule._
        0 * _

        expect:
        !result
    }

    def 'isNamedConstructor asks whether the result type names the declaring type or one of its interfaces'() {
        StaticMethodsModifyStaticState rule = Spy()
        ASTClassType resultType = Mock()
        ASTTypeDeclaration declaring = Mock()

        when:
        def result = rule.isNamedConstructor(method)

        then:
        1 * method.resultTypeNode >> resultType
        1 * resultType.simpleName >> 'Widget'
        1 * method.enclosingType >> declaring
        1 * rule.namesDeclaringTypeOrItsInterfaces(declaring, 'Widget') >> named
        1 * rule._
        0 * _

        expect:
        result == named

        where:
        named << [true, false]
    }

    def 'namesDeclaringTypeOrItsInterfaces is true for the declaring type itself'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        ASTTypeDeclaration type = Mock()

        when:
        def result = rule.namesDeclaringTypeOrItsInterfaces(type, 'Widget')

        then:
        1 * type.simpleName >> 'Widget'
        0 * _

        expect:
        result
    }

    def 'namesDeclaringTypeOrItsInterfaces is true for an interface the type directly declares'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        ASTTypeDeclaration type = Mock()
        ASTClassType iface = Mock()

        when:
        def result = rule.namesDeclaringTypeOrItsInterfaces(type, 'Runnable')

        then:
        1 * type.simpleName >> 'Widget'
        1 * type.superInterfaceTypeNodes >> stream([iface])
        1 * iface.simpleName >> 'Runnable'
        0 * _

        expect:
        result
    }

    def 'namesDeclaringTypeOrItsInterfaces is false for any other name'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        ASTTypeDeclaration type = Mock()
        ASTClassType iface = Mock()

        when:
        def result = rule.namesDeclaringTypeOrItsInterfaces(type, 'Runnable')

        then:
        1 * type.simpleName >> 'Widget'
        1 * type.superInterfaceTypeNodes >> stream([iface])
        1 * iface.simpleName >> 'Serializable'
        0 * _

        expect:
        !result
    }

    def 'declaredInUtilityClass is decided by the marker, then by the shape of the type'() {
        StaticMethodsModifyStaticState rule = Spy()
        ASTTypeDeclaration type = Mock()

        when:
        def result = rule.declaredInUtilityClass(method)

        then:
        1 * method.enclosingType >> type
        1 * rule.hasAnyAnnotation(type, ['UtilityClass'] as Set) >> marked
        (marked ? 0 : 1) * rule.declaresNoInstanceMethod(type) >> noInstanceMethod
        (!marked && noInstanceMethod ? 1 : 0) * rule.declaresNoAccessibleConstructor(type) >> noAccessibleConstructor
        1 * rule._
        0 * _

        expect:
        result == expected

        where:
        marked | noInstanceMethod | noAccessibleConstructor | expected
        true   | false            | false                   | true
        false  | true             | true                    | true
        false  | true             | false                   | false
        false  | false            | true                    | false
        false  | false            | false                   | false
    }

    def 'declaresNoInstanceMethod is true for a type with no methods'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        ASTTypeDeclaration type = Mock()

        when:
        def result = rule.declaresNoInstanceMethod(type)

        then:
        1 * type.getDeclarations(ASTMethodDeclaration) >> stream([])
        0 * _

        expect:
        result
    }

    def 'declaresNoInstanceMethod is true for a type whose methods are all static'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        ASTTypeDeclaration type = Mock()
        ASTMethodDeclaration first = Mock()
        ASTMethodDeclaration second = Mock()

        when:
        def result = rule.declaresNoInstanceMethod(type)

        then:
        1 * type.getDeclarations(ASTMethodDeclaration) >> stream([first, second])
        1 * first.static >> true
        1 * second.static >> true
        0 * _

        expect:
        result
    }

    def 'declaresNoInstanceMethod is false once a method is an instance method'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        ASTTypeDeclaration type = Mock()
        ASTMethodDeclaration instance = Mock()
        ASTMethodDeclaration later = Mock()

        when:
        def result = rule.declaresNoInstanceMethod(type)

        then:
        1 * type.getDeclarations(ASTMethodDeclaration) >> stream([instance, later])
        1 * instance.static >> false
        0 * _

        expect:
        !result
    }

    def 'declaresNoAccessibleConstructor falls back to the implicit constructor when none is declared'() {
        StaticMethodsModifyStaticState rule = Spy()
        ASTTypeDeclaration type = Mock()

        when:
        def result = rule.declaresNoAccessibleConstructor(type)

        then:
        1 * type.getDeclarations(ASTConstructorDeclaration) >> stream([])
        1 * rule.hasInaccessibleImplicitConstructor(type) >> true
        1 * rule._
        0 * _

        expect:
        result
    }

    def 'declaresNoAccessibleConstructor is true when no declared constructor is accessible'() {
        StaticMethodsModifyStaticState rule = Spy()
        ASTTypeDeclaration type = Mock()
        ASTConstructorDeclaration constructor = Mock()

        when:
        def result = rule.declaresNoAccessibleConstructor(type)

        then:
        1 * type.getDeclarations(ASTConstructorDeclaration) >> stream([constructor])
        1 * rule.isAccessible(constructor) >> false
        1 * rule._
        0 * _

        expect:
        result
    }

    def 'declaresNoAccessibleConstructor is false when a declared constructor is accessible'() {
        StaticMethodsModifyStaticState rule = Spy()
        ASTTypeDeclaration type = Mock()
        ASTConstructorDeclaration constructor = Mock()

        when:
        def result = rule.declaresNoAccessibleConstructor(type)

        then:
        1 * type.getDeclarations(ASTConstructorDeclaration) >> stream([constructor])
        1 * rule.isAccessible(constructor) >> true
        1 * rule._
        0 * _

        expect:
        !result
    }

    /** {@code interface} and {@code enum} are keywords, so {@code isInterface()} and {@code isEnum()} have no property form. */
    @SuppressWarnings('UnnecessaryGetter')
    def 'hasInaccessibleImplicitConstructor is true for an interface, an enum, or a type below protected'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        ASTTypeDeclaration type = Mock()

        when:
        def result = rule.hasInaccessibleImplicitConstructor(type)

        then:
        1 * type.isInterface() >> isInterface
        (isInterface ? 0 : 1) * type.isEnum() >> isEnum
        (isInterface || isEnum ? 0 : 1) * type.visibility >> visibility
        0 * _

        expect:
        result == expected

        where:
        isInterface | isEnum | visibility          | expected
        true        | false  | Visibility.V_PUBLIC | true
        false       | true   | Visibility.V_PUBLIC | true
        false       | false  | Visibility.V_PUBLIC | false
        false       | false  | Visibility.V_PROTECTED | false
        false       | false  | Visibility.V_PACKAGE   | true
    }

    def 'isAccessible is true for a constructor at protected or wider'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        ASTConstructorDeclaration constructor = Mock()

        when:
        def result = rule.isAccessible(constructor)

        then:
        1 * constructor.visibility >> visibility
        0 * _

        expect:
        result == expected

        where:
        visibility             | expected
        Visibility.V_PUBLIC    | true
        Visibility.V_PROTECTED | true
        Visibility.V_PACKAGE   | false
        Visibility.V_PRIVATE   | false
    }

    def 'writesPrivateStaticField is true when a named reference in the method writes one'() {
        StaticMethodsModifyStaticState rule = Spy()
        ASTNamedReferenceExpr reference = Mock()

        when:
        def result = rule.writesPrivateStaticField(method)

        then:
        1 * method.descendants() >> stream([reference], NodeStream.DescendantNodeStream)
        1 * rule.isWriteToPrivateStaticField(reference) >> writes
        1 * rule._
        0 * _

        expect:
        result == writes

        where:
        writes << [true, false]
    }

    def 'isWriteToPrivateStaticField needs a write to a private static field'() {
        StaticMethodsModifyStaticState rule = Spy()
        ASTNamedReferenceExpr access = Mock()
        JVariableSymbol symbol = Mock()

        when:
        def result = rule.isWriteToPrivateStaticField(access)

        then:
        1 * access.accessType >> accessType
        (accessType == ASTAssignableExpr.AccessType.WRITE ? 1 : 0) * access.referencedSym >> symbol
        (accessType == ASTAssignableExpr.AccessType.WRITE ? 1 : 0) * rule.isPrivateStaticField(symbol) >> privateStatic
        1 * rule._
        0 * _

        expect:
        result == expected

        where:
        accessType                              | privateStatic | expected
        ASTAssignableExpr.AccessType.READ       | true          | false
        ASTAssignableExpr.AccessType.WRITE      | true          | true
        ASTAssignableExpr.AccessType.WRITE      | false         | false
    }

    def 'isPrivateStaticField is false for a missing symbol'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()

        expect:
        !rule.isPrivateStaticField(null)
    }

    def 'isPrivateStaticField is false for a symbol that is not a field'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        JVariableSymbol symbol = Mock()

        expect:
        !rule.isPrivateStaticField(symbol)
    }

    def 'isPrivateStaticField is true only for a field that is both static and private'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        JFieldSymbol field = Mock()

        when:
        def result = rule.isPrivateStaticField(field)

        then:
        1 * field.static >> isStatic
        (isStatic ? 1 : 0) * field.modifiers >> modifiers
        0 * _

        expect:
        result == expected

        where:
        isStatic | modifiers                  | expected
        true     | Modifier.PRIVATE           | true
        false    | Modifier.PRIVATE           | false
        true     | Modifier.PUBLIC            | false
    }

    def 'hasAnyAnnotation is true when an annotation has one of the given simple names'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        Annotatable node = Mock(ASTMethodDeclaration)
        net.sourceforge.pmd.lang.java.ast.ASTAnnotation annotation = Mock()

        when:
        def result = rule.hasAnyAnnotation(node, ['BeforeAll', 'AfterAll'] as Set)

        then:
        1 * node.declaredAnnotations >> stream([annotation])
        1 * annotation.simpleName >> 'AfterAll'
        0 * _

        expect:
        result
    }

    def 'hasAnyAnnotation is false when no annotation has one of the given simple names'() {
        StaticMethodsModifyStaticState rule = new StaticMethodsModifyStaticState()
        Annotatable node = Mock(ASTMethodDeclaration)
        net.sourceforge.pmd.lang.java.ast.ASTAnnotation annotation = Mock()

        when:
        def result = rule.hasAnyAnnotation(node, ['BeforeAll', 'AfterAll'] as Set)

        then:
        1 * node.declaredAnnotations >> stream([annotation])
        1 * annotation.simpleName >> 'Test'
        0 * _

        expect:
        !result
    }

    /**
     * A stream over the given items whose operations run the predicate or class they are given, so the
     * rule's own lambdas and method references are exercised rather than replaced. Coerced to the type a
     * rule declares, so a call on a {@code DescendantNodeStream} resolves.
     */
    private static NodeStream stream(List items, Class<? extends NodeStream> type = NodeStream) {
        [
                any: { Predicate matches -> items.any { matches.test(it) } },
                all: { Predicate matches -> items.every { matches.test(it) } },
                none: { Predicate matches -> items.every { !matches.test(it) } },
                isEmpty: { items.empty },
                filterIs: { Class<?> kind -> stream(items.findAll { kind.isInstance(it) }) },
        ].asType(type)
    }
}
