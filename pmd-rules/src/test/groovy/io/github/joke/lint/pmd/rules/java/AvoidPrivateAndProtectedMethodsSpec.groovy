package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('unit')
class AvoidPrivateAndProtectedMethodsSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('AvoidPrivateAndProtectedMethods', fixture, version) == lines

        where:
        fixture                                                                  | version | lines
        'APrivateMethodIsReported'                                               | null | [2]
        'AProtectedMethodIsReported'                                             | null | [2]
        'APackagePrivateMethodIsNotReported'                                     | null | []
        'APublicMethodIsNotReported'                                             | null | []
        'AnInterfaceMethodIsImplicitlyPublicAndIsNotReported'                    | null | []
        'APrivateInterfaceMethodIsReported'                                      | null | [4]
        'APrivateConstructorIsNotReportedConstructorsAreOutOfScope'              | null | []
        'AProtectedConstructorIsNotReported'                                     | null | []
        'AProtectedOverrideIsNotReportedVisibilityIsFixedByTheSupertype'         | null | []
        'AProtectedMethodWithoutOverrideIsReported'                              | null | [2]
        'APrivateStaticMethodIsLeftToStaticMethodsModifyStaticState'             | null | []
        'APrivateStaticMethodInAUtilityClassIsReportedByNeitherRule'             | null | []
        'AProtectedMethodWithASubclassInTheSameFileIsStillReported'              | null | [2]
        'SuppressionIsAvailableForAGenuineExtensionPoint'                        | null | []
        'AnUnrelatedAnnotationDoesNotExemptAPrivateMethod'                       | null | [3]
        'AProtectedExtensionPointMarkedOverrideOnlyIsNotReported'                | null | []
        'TheSameMarkerQualifiedThroughItsOuterTypeIsAlsoMatched'                 | null | []
        'AProtectedTestSeamMarkedVisibleForTestingIsNotReported'                 | null | []
        'AnUnmarkedProtectedMethodBesideAMarkedOneIsStillReported'               | null | [5]
        'AMarkerDoesNotExcuseAPrivateMethodNoAnnotationMakesOneReachable'        | null | [3, 6]
        'AnUnrelatedAnnotationDoesNotExcuseAProtectedMethod'                     | null | [3]
        'AMarkedProtectedMethodInAFinalClassIsLeftToTheStockRuleNotReportedHere' | null | []
        'SeveralHiddenMethodsInOneClassAreEachReported'                          | null | [2, 3]
    }
}
