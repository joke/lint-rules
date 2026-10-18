package io.github.joke.lint.pmd.rules.java

import net.sourceforge.pmd.lang.java.ast.ASTClassType
import spock.lang.Specification
import spock.lang.Tag

import static net.sourceforge.pmd.lang.document.Chars.wrap

/**
 * Covers the decisions the {@code pmd-test} fixtures cannot reach.
 *
 * Both import rules guard against inputs that valid Java never produces: a qualifier with no class
 * type, a name with no dot, a prefix exactly as long as the member. Those guards are correct and
 * unreachable through parsed source, so a fixture can neither exercise them nor kill their mutants.
 * Calling the predicates directly is what the package-private seam is for.
 */
@Tag('unit')
class ImportRulePredicatesSpec extends Specification {

    def staticImports = new UseStaticImports()
    def typeImports = new UseTypeImports()

    def 'a type with no recorded name is not unambiguous'() {
        expect:
        !typeImports.isUnambiguous('a.B', [:])
    }

    def 'an unqualified name has an empty qualifier'() {
        expect:
        typeImports.qualifierOf('Foo') == ''
    }

    def 'a qualified name splits at the last dot'() {
        expect:
        typeImports.qualifierOf('java.util.List') == 'java.util'
    }

    def 'a simple name is the segment after the last dot'() {
        expect:
        typeImports.simpleNameOf('java.util.Map.Entry') == 'Entry'
    }

    def 'type arguments are trimmed from a qualified name'() {
        ASTClassType type = Mock()

        when:
        def name = typeImports.qualifiedName(type)

        then:
        1 * type.text >> wrap('java.util.List<String>')
        0 * _

        expect:
        name == 'java.util.List'
    }

    def 'surrounding whitespace is trimmed from a qualified name'() {
        ASTClassType type = Mock()

        when:
        def name = typeImports.qualifiedName(type)

        then:
        1 * type.text >> wrap('  java.util.List  ')
        0 * _

        expect:
        name == 'java.util.List'
    }

    def 'a type in the current package is already in scope'() {
        expect:
        typeImports.isAlreadyInScope('com.example.Thing', [] as Set, 'com.example')
    }

    def 'a type in another package is not already in scope'() {
        expect:
        !typeImports.isAlreadyInScope('com.other.Thing', [] as Set, 'com.example')
    }
}
