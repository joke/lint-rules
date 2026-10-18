package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.ast.NodeStream
import net.sourceforge.pmd.lang.java.ast.ASTClassType
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.ast.ASTBlock
import net.sourceforge.pmd.lang.java.ast.ASTImportDeclaration
import net.sourceforge.pmd.lang.java.ast.JavaNode
import net.sourceforge.pmd.reporting.RuleContext

import java.util.function.Predicate
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

import static net.sourceforge.pmd.lang.document.Chars.wrap

/**
 * Each {@code @VisibleForTesting} method of {@link UseTypeImports} is tested directly. The method under
 * test is the spy's real method; the siblings it calls are stubbed, so each feature sees one decision.
 */
@Tag('unit')
@Subject(UseTypeImports)
class UseTypeImportsSpec extends Specification {

    ASTCompilationUnit unit = Mock()
    RuleContext context = Mock()

    def 'visit delegates to the report decision and returns the data it was given'() {
        UseTypeImports rule = Spy()

        when:
        def result = rule.visit(unit, context)

        then:
        1 * rule.reportImportableTypes(unit, context) >> {}
        1 * rule._
        0 * _

        expect:
        result.is(context)
    }

    def 'reportImportableTypes collects the qualified types, then reports the unambiguous ones'() {
        UseTypeImports rule = Spy()

        when:
        rule.reportImportableTypes(unit, context)

        then:
        1 * rule.collectQualifiedTypes(unit, [:], [:]) >> {}
        1 * rule.reportUnambiguousTypes(unit, [:], [:], context) >> {}
        1 * rule._
        0 * _
    }

    def 'collectQualifiedTypes records every class type in the unit'() {
        UseTypeImports rule = Spy()
        ASTClassType type = Mock()
        Map namesBySimpleName = [:]
        Map firstUseByName = [:]

        when:
        rule.collectQualifiedTypes(unit, namesBySimpleName, firstUseByName)

        then:
        1 * unit.descendants(ASTClassType) >> ([type] as NodeStream.DescendantNodeStream)
        1 * rule.recordQualifiedType(type, namesBySimpleName, firstUseByName) >> {}
        1 * rule._
        0 * _
    }

    def 'recordQualifiedType records nothing for a use that is not a qualified use'() {
        UseTypeImports rule = Spy()
        ASTClassType type = Mock()
        Map namesBySimpleName = [:]
        Map firstUseByName = [:]

        when:
        rule.recordQualifiedType(type, namesBySimpleName, firstUseByName)

        then:
        1 * rule.isQualifiedUse(type) >> false
        1 * rule._
        0 * _

        expect:
        namesBySimpleName == [:]
        firstUseByName == [:]
    }

    def 'recordQualifiedType records the name under its simple name, with its first use'() {
        UseTypeImports rule = Spy()
        ASTClassType type = Mock()
        Map namesBySimpleName = [:]
        Map firstUseByName = [:]

        when:
        rule.recordQualifiedType(type, namesBySimpleName, firstUseByName)

        then:
        1 * rule.isQualifiedUse(type) >> true
        1 * rule.qualifiedName(type) >> 'java.util.List'
        1 * type.simpleName >> 'List'
        1 * rule._
        0 * _

        expect:
        namesBySimpleName == [List: ['java.util.List'] as Set]
        firstUseByName == ['java.util.List': type]
    }

    def 'isQualifiedUse is a root use outside another class type, and is decided by the root flag'() {
        UseTypeImports rule = Spy()
        ASTClassType type = Mock()

        when:
        def result = rule.isQualifiedUse(type)

        then:
        1 * type.parent >> Mock(parentType)
        (parentType == ASTClassType ? 0 : 1) * rule.isRootFullyQualified(type) >> rootFullyQualified
        1 * rule._
        0 * _

        expect:
        result == expected

        where:
        parentType    | rootFullyQualified | expected
        ASTClassType  | false              | false
        ASTBlock      | true               | true
        ASTBlock      | false              | false
    }

    def 'isRootFullyQualified reads the flag on a type with no qualifier'() {
        UseTypeImports rule = new UseTypeImports()
        ASTClassType type = Mock()

        when:
        def result = rule.isRootFullyQualified(type)

        then:
        1 * type.qualifier >> null
        1 * type.fullyQualified >> fullyQualified
        0 * _

        expect:
        result == fullyQualified

        where:
        fullyQualified << [true, false]
    }

    def 'isRootFullyQualified walks to the qualifier of a qualified type'() {
        UseTypeImports rule = Spy()
        ASTClassType type = Mock()
        ASTClassType qualifier = Mock()

        when:
        def result = rule.isRootFullyQualified(type)

        then:
        1 * type.qualifier >> qualifier
        1 * rule.isRootFullyQualified(qualifier) >> true
        1 * rule._
        0 * _

        expect:
        result
    }

    def 'qualifiedName is the source text without type arguments or surrounding whitespace'() {
        UseTypeImports rule = new UseTypeImports()
        ASTClassType type = Mock()

        when:
        def name = rule.qualifiedName(type)

        then:
        1 * type.text >> wrap(text)
        0 * _

        expect:
        name == expected

        where:
        text                         | expected
        'java.util.List'             | 'java.util.List'
        'java.util.List<String>'     | 'java.util.List'
        '  java.util.List  '         | 'java.util.List'
    }

    def 'reportUnambiguousTypes reports each first use that is importable'() {
        UseTypeImports rule = Spy()
        JavaNode use = Mock()
        Map namesBySimpleName = [List: ['java.util.List'] as Set]
        Map firstUseByName = ['java.util.List': use]
        Set imported = ['Map'] as Set

        when:
        rule.reportUnambiguousTypes(unit, namesBySimpleName, firstUseByName, context)

        then:
        1 * rule.importedSimpleNames(unit) >> imported
        1 * unit.packageName >> 'com.example'
        1 * rule.reportIfImportable('java.util.List', use, namesBySimpleName, imported, 'com.example', context) >> {}
        1 * rule._
        0 * _
    }

    def 'reportIfImportable adds a violation for an importable name'() {
        UseTypeImports rule = Spy()
        JavaNode use = Mock()
        Map namesBySimpleName = [List: ['java.util.List'] as Set]
        Set imported = [] as Set

        when:
        rule.reportIfImportable('java.util.List', use, namesBySimpleName, imported, 'com.example', context)

        then:
        1 * rule.isImportable('java.util.List', namesBySimpleName, imported, 'com.example') >> true
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(use, 'java.util.List')
        0 * _
    }

    def 'reportIfImportable adds no violation otherwise'() {
        UseTypeImports rule = Spy()
        JavaNode use = Mock()
        Map namesBySimpleName = [List: ['java.util.List'] as Set]
        Set imported = [] as Set

        when:
        rule.reportIfImportable('java.util.List', use, namesBySimpleName, imported, 'com.example', context)

        then:
        1 * rule.isImportable('java.util.List', namesBySimpleName, imported, 'com.example') >> false
        1 * rule._
        0 * _
    }

    def 'isImportable needs a name that is not already in scope and is unambiguous'() {
        UseTypeImports rule = Spy()
        Map namesBySimpleName = [List: ['java.util.List'] as Set]
        Set imported = [] as Set

        when:
        def result = rule.isImportable('java.util.List', namesBySimpleName, imported, 'com.example')

        then:
        1 * rule.isAlreadyInScope('java.util.List', imported, 'com.example') >> inScope
        (inScope ? 0 : 1) * rule.isUnambiguous('java.util.List', namesBySimpleName) >> unambiguous
        1 * rule._
        0 * _

        expect:
        result == expected

        where:
        inScope | unambiguous | expected
        true    | true        | false
        false   | false       | false
        false   | true        | true
    }

    def 'isAlreadyInScope is true for an imported simple name, java.lang, or the current package'() {
        UseTypeImports rule = new UseTypeImports()

        expect:
        rule.isAlreadyInScope(name, imported as Set, 'com.example') == expected

        where:
        name                    | imported | expected
        'java.util.List'        | ['List'] | true
        'java.lang.String'      | []       | true
        'com.example.Thing'     | []       | true
        'com.other.Thing'       | []       | false
    }

    def 'isUnambiguous needs exactly one type recorded under the simple name'() {
        UseTypeImports rule = new UseTypeImports()

        expect:
        rule.isUnambiguous(name, namesBySimpleName) == expected

        where:
        name              | namesBySimpleName                                                     | expected
        'java.util.List'  | [List: ['java.util.List'] as Set]                                      | true
        'java.util.List'  | [List: ['java.util.List', 'java.awt.List'] as Set]                     | false
        'java.util.List'  | [:]                                                                    | false
    }

    def 'simpleNameOf is the segment after the last dot'() {
        UseTypeImports rule = new UseTypeImports()

        expect:
        rule.simpleNameOf(name) == expected

        where:
        name                     | expected
        'java.util.Map.Entry'    | 'Entry'
        'List'                   | 'List'
    }

    def 'qualifierOf is everything before the last dot, or empty for a simple name'() {
        UseTypeImports rule = new UseTypeImports()

        expect:
        rule.qualifierOf(name) == expected

        where:
        name                     | expected
        'Foo'                    | ''
        'java.util.List'         | 'java.util'
        'java.util.Map.Entry'    | 'java.util.Map'
    }

    def 'importedSimpleNames holds the simple name of every non-static import'() {
        UseTypeImports rule = new UseTypeImports()
        ASTImportDeclaration typeImport = Mock()
        ASTImportDeclaration staticImport = Mock()
        NodeStream.DescendantNodeStream imports = [
                filter: { Predicate keep -> [typeImport, staticImport].findAll { keep.test(it) } as NodeStream }
        ] as NodeStream.DescendantNodeStream

        when:
        def names = rule.importedSimpleNames(unit)

        then:
        1 * unit.descendants(ASTImportDeclaration) >> imports
        1 * typeImport.static >> false
        1 * staticImport.static >> true
        1 * typeImport.importedSimpleName >> 'List'
        0 * _

        expect:
        names == ['List'] as Set
    }
}
