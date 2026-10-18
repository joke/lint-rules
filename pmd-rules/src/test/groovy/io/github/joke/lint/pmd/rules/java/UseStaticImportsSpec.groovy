package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.ast.NodeStream
import net.sourceforge.pmd.lang.java.ast.ASTClassType
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit
import net.sourceforge.pmd.lang.java.ast.ASTExpression
import net.sourceforge.pmd.lang.java.ast.ASTFieldAccess
import net.sourceforge.pmd.lang.java.ast.ASTMethodCall
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration
import net.sourceforge.pmd.lang.java.ast.ASTTypeExpression
import net.sourceforge.pmd.lang.java.ast.ASTVariableId
import net.sourceforge.pmd.lang.java.ast.JavaNode
import net.sourceforge.pmd.reporting.RuleContext
import spock.lang.Specification
import spock.lang.Tag

/**
 * Each {@code @VisibleForTesting} method of {@link UseStaticImports} is tested directly. The method under
 * test is the spy's real method; the siblings it calls are stubbed, so each feature sees one decision.
 */
@Tag('unit')
class UseStaticImportsSpec extends Specification {

    ASTCompilationUnit unit = Mock()
    RuleContext context = Mock()

    def 'visit delegates to the report decision and returns the data it was given'() {
        UseStaticImports rule = Spy()

        when:
        def result = rule.visit(unit, context)

        then:
        1 * rule.reportImportableMembers(unit, context) >> {}
        1 * rule._
        0 * _

        expect:
        result.is(context)
    }

    def 'reportImportableMembers collects the accesses, then reports the unambiguous members'() {
        UseStaticImports rule = Spy()

        when:
        rule.reportImportableMembers(unit, context)

        then:
        1 * rule.collectQualifiedAccesses(unit, [:], [:]) >> {}
        1 * rule.reportUnambiguousMembers(unit, [:], [:], context) >> {}
        1 * rule._
        0 * _
    }

    def 'collectQualifiedAccesses records every method call and field access'() {
        UseStaticImports rule = Spy()
        ASTMethodCall call = Mock()
        ASTFieldAccess access = Mock()
        ASTExpression callQualifier = Mock()
        ASTExpression accessQualifier = Mock()
        Map owners = [:]
        Map firstUses = [:]

        when:
        rule.collectQualifiedAccesses(unit, owners, firstUses)

        then:
        1 * unit.descendants(ASTMethodCall) >> ([call] as NodeStream.DescendantNodeStream)
        1 * call.qualifier >> callQualifier
        1 * call.methodName >> 'copyOf'
        1 * rule.recordAccess(callQualifier, 'copyOf', call, owners, firstUses) >> {}
        1 * unit.descendants(ASTFieldAccess) >> ([access] as NodeStream.DescendantNodeStream)
        1 * access.qualifier >> accessQualifier
        1 * access.name >> 'MAX_VALUE'
        1 * rule.recordAccess(accessQualifier, 'MAX_VALUE', access, owners, firstUses) >> {}
        1 * rule._
        0 * _
    }

    def 'recordAccess records a member under its owner, with its first use'() {
        UseStaticImports rule = Spy()
        JavaNode qualifier = Mock()
        JavaNode use = Mock()
        Map owners = [:]
        Map firstUses = [:]

        when:
        rule.recordAccess(qualifier, 'copyOf', use, owners, firstUses)

        then:
        1 * rule.qualifyingTypeName(qualifier) >> 'List'
        1 * rule._
        0 * _

        expect:
        owners == [copyOf: ['List'] as Set]
        firstUses == [copyOf: use]
    }

    def 'recordAccess keeps the first use of a member already recorded'() {
        UseStaticImports rule = Spy()
        JavaNode qualifier = Mock()
        JavaNode earlier = Mock()
        JavaNode later = Mock()
        Map owners = [copyOf: ['List'] as Set]
        Map firstUses = [copyOf: earlier]

        when:
        rule.recordAccess(qualifier, 'copyOf', later, owners, firstUses)

        then:
        1 * rule.qualifyingTypeName(qualifier) >> 'Collections'
        1 * rule._
        0 * _

        expect:
        owners == [copyOf: ['List', 'Collections'] as Set]
        firstUses == [copyOf: earlier]
    }

    def 'recordAccess records nothing for a qualifier with no type name'() {
        UseStaticImports rule = Spy()
        JavaNode qualifier = Mock()
        JavaNode use = Mock()
        Map owners = [:]
        Map firstUses = [:]

        when:
        rule.recordAccess(qualifier, 'copyOf', use, owners, firstUses)

        then:
        1 * rule.qualifyingTypeName(qualifier) >> null
        1 * rule._
        0 * _

        expect:
        owners == [:]
        firstUses == [:]
    }

    def 'qualifyingTypeName is null for a qualifier that is not a type expression'() {
        UseStaticImports rule = new UseStaticImports()

        expect:
        rule.qualifyingTypeName(null) == null
    }

    def 'qualifyingTypeName is null for a type expression without a class type'() {
        UseStaticImports rule = new UseStaticImports()
        ASTTypeExpression qualifier = Mock()

        when:
        def name = rule.qualifyingTypeName(qualifier)

        then:
        1 * qualifier.firstChild(ASTClassType) >> null
        0 * _

        expect:
        name == null
    }

    def 'qualifyingTypeName is the simple name of the class type'() {
        UseStaticImports rule = new UseStaticImports()
        ASTTypeExpression qualifier = Mock()
        ASTClassType type = Mock()

        when:
        def name = rule.qualifyingTypeName(qualifier)

        then:
        1 * qualifier.firstChild(ASTClassType) >> type
        1 * type.simpleName >> 'Collectors'
        0 * _

        expect:
        name == 'Collectors'
    }

    def 'reportUnambiguousMembers reports each first use with its member'() {
        UseStaticImports rule = Spy()
        JavaNode ofUse = Mock()
        JavaNode copyUse = Mock()
        Map owners = [of: ['Range'] as Set, copyOf: ['List'] as Set]
        Map firstUses = [of: ofUse, copyOf: copyUse]
        Set declared = ['local'] as Set

        when:
        rule.reportUnambiguousMembers(unit, owners, firstUses, context)

        then:
        1 * rule.declaredNames(unit) >> declared
        1 * rule.reportIfImportable('of', ofUse, owners, declared, context) >> {}
        1 * rule.reportIfImportable('copyOf', copyUse, owners, declared, context) >> {}
        1 * rule._
        0 * _
    }

    def 'reportIfImportable adds a violation for an importable member'() {
        UseStaticImports rule = Spy()
        JavaNode use = Mock()
        Map owners = [copyOf: ['List'] as Set]
        Set declared = [] as Set

        when:
        rule.reportIfImportable('copyOf', use, owners, declared, context)

        then:
        1 * rule.isImportable('copyOf', owners, declared) >> true
        1 * rule._
        _ * context.rule >> rule
        1 * context.addViolation(use, 'copyOf')
        0 * _
    }

    def 'reportIfImportable adds no violation otherwise'() {
        UseStaticImports rule = Spy()
        JavaNode use = Mock()
        Map owners = [copyOf: ['List'] as Set]
        Set declared = [] as Set

        when:
        rule.reportIfImportable('copyOf', use, owners, declared, context)

        then:
        1 * rule.isImportable('copyOf', owners, declared) >> false
        1 * rule._
        0 * _
    }

    def 'isImportable needs a name that is long enough, informative and unambiguous'() {
        UseStaticImports rule = Spy()
        Map owners = [copyOf: ['List'] as Set]
        Set declared = [] as Set

        when:
        def result = rule.isImportable('copyOf', owners, declared)

        then:
        1 * rule.isLongEnough('copyOf') >> longEnough
        (longEnough ? 1 : 0) * rule.isUninformative('copyOf') >> uninformative
        (longEnough && !uninformative ? 1 : 0) * rule.isUnambiguous('copyOf', owners, declared) >> unambiguous
        1 * rule._
        0 * _

        expect:
        result == expected

        where:
        longEnough | uninformative | unambiguous | expected
        false      | false         | true        | false
        true       | true          | true        | false
        true       | false         | false       | false
        true       | false         | true        | true
    }

    def 'isLongEnough is false below the four-character floor'() {
        UseStaticImports rule = new UseStaticImports()

        expect:
        !rule.isLongEnough('max')
        rule.isLongEnough('sqrt')
    }

    def 'isUninformative is true for an excluded name or a factory name'() {
        UseStaticImports rule = new UseStaticImports()

        expect:
        rule.isUninformative(name) == expected

        where:
        name        | expected
        'value'     | true
        'INSTANCE'  | true
        'ofSeconds' | true
        'toList'    | false
    }

    def 'hasFactoryPrefix is true for a name that starts with a factory prefix at a camel-case boundary'() {
        UseStaticImports rule = new UseStaticImports()

        expect:
        rule.hasFactoryPrefix(name) == expected

        where:
        name         | expected
        'ofSeconds'  | true
        'fromString' | true
        'offer'      | false
        'list'       | false
    }

    def 'startsFactoryName needs the prefix, a longer name, and an uppercase letter after it'() {
        UseStaticImports rule = new UseStaticImports()

        expect:
        rule.startsFactoryName(member, prefix) == expected

        where:
        member       | prefix | expected
        'ofSeconds'  | 'of'   | true
        'of'         | 'of'   | false
        'offer'      | 'of'   | false
        'fromString' | 'from' | true
    }

    def 'isUnambiguous needs exactly one owner and no local binding of the name'() {
        UseStaticImports rule = new UseStaticImports()

        expect:
        rule.isUnambiguous('sqrt', owners, declared) == expected

        where:
        owners                                 | declared | expected
        [sqrt: ['Math'] as Set]                | [] as Set       | true
        [:]                                    | [] as Set       | false
        [sqrt: ['Math', 'StrictMath'] as Set]  | [] as Set       | false
        [sqrt: ['Math'] as Set]                | ['sqrt'] as Set | false
    }

    def 'declaredNames holds every method and variable name in the unit'() {
        UseStaticImports rule = new UseStaticImports()
        ASTMethodDeclaration method = Mock()
        ASTVariableId variable = Mock()

        when:
        def names = rule.declaredNames(unit)

        then:
        1 * unit.descendants(ASTMethodDeclaration) >> ([method] as NodeStream.DescendantNodeStream)
        1 * method.name >> 'run'
        1 * unit.descendants(ASTVariableId) >> ([variable] as NodeStream.DescendantNodeStream)
        1 * variable.name >> 'limit'
        0 * _

        expect:
        names == ['run', 'limit'] as Set
    }
}
