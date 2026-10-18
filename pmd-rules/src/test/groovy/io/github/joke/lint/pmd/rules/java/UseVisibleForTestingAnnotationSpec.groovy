package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('unit')
class UseVisibleForTestingAnnotationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('UseVisibleForTestingAnnotation', fixture, version) == lines

        where:
        fixture                                                                       | version | lines
        'AnUnannotatedPackagePrivateMethodIsReported'                                 | null | [2]
        'AnAnnotatedPackagePrivateMethodIsNotReported'                                | null | []
        'TheJetBrainsAnnotationIsRecognised'                                          | null | []
        'TheGuavaAnnotationIsRecognised'                                              | null | []
        'ADifferentAnnotationIsNotRecognised'                                         | null | [3]
        'APublicMethodIsNotReported'                                                  | null | []
        'APrivateMethodIsNotReportedAvoidPrivateAndProtectedMethodsOwnsThatCase'      | null | []
        'AProtectedMethodIsNotReportedAvoidPrivateAndProtectedMethodsOwnsThatCaseToo' | null | []
        'APackagePrivateFieldIsNotReportedOnlyMethodsAreInScope'                      | null | []
        'APackagePrivateConstructorIsNotReported'                                     | null | []
        'APackagePrivateNestedClassIsNotReported'                                     | null | []
        'APackagePrivateOverrideIsNotReported'                                        | null | []
        'AJUnitTestMethodIsNotReported'                                               | null | []
        'JUnitLifecycleMethodsAreNotReported'                                         | null | []
        'ATestFactoryAndATestTemplateAreNotReported'                                  | null | []
        'AnUnannotatedHelperInATestClassIsReported'                                   | null | [5]
        'AnInterfaceMethodIsImplicitlyPublicAndIsNotReported'                         | null | []
        'APackagePrivateStaticMethodIsStillAnUnmarkedSeam'                            | null | [4]
        'SeveralUnmarkedSeamsInOneClassAreEachReported'                               | null | [2, 3]
    }
}
