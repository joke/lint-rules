package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('unit')
class UseTypeImportsSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('UseTypeImports', fixture, version) == lines

        where:
        fixture                                                     | version | lines
        'AFullyQualifiedTypeInADeclarationIsReported'               | null | [2]
        'AFullyQualifiedTypeInAGenericArgumentIsReported'           | null | [4]
        'AFullyQualifiedReturnTypeIsReported'                       | null | [2]
        'ASimpleNameAlreadyImportedIsNotReported'                   | null | []
        'AnImportDeclarationIsNotItselfReported'                    | null | []
        'AJavaLangTypeIsLeftToTheStockRule'                         | null | []
        'ARedundantlyQualifiedImportedTypeIsLeftToTheStockRule'     | null | []
        'TwoTypesWithTheSameSimpleNameAreBothLeftAlone'             | null | []
        'AQualifiedTypeCollidingWithAnExistingImportIsNotReported'  | null | []
        'RepeatedUseOfOneQualifiedTypeYieldsOneViolation'           | null | [2]
        'TwoQualifiedTypesYieldTwoViolations'                       | null | [2, 3]
        'AQualifiedNestedTypeIsReported'                            | null | [2]
        'ANestedTypeReachedThroughAnImportedOuterTypeIsNotReported' | null | []
        'AFullyQualifiedSupertypeIsReported'                        | null | [1]
        'AFullyQualifiedAnnotationIsReported'                       | null | [1]
        'ATypeInTheCurrentPackageIsNotReported'                     | null | []
        'ATypeInAnotherPackageIsReportedEvenWhenAPackageIsDeclared' | null | [4]
        'AStaticImportDoesNotBindASimpleNameForTypeResolution'      | null | [4]
        'AStaticImportOfTheSameSimpleNameDoesNotPutATypeInScope'    | null | [4]
        'SuppressionIsAvailableAtTheSite'                           | null | []
    }
}
