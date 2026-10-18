package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('unit')
class StaticMethodsModifyStaticStateSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('StaticMethodsModifyStaticState', fixture, version) == lines

        where:
        fixture                                                                          | version | lines
        'AStaticHelperIsReported'                                                        | null | [2]
        'AnInstanceMethodIsNotReported'                                                  | null | []
        'AStaticFactoryReturningTheEnclosingTypeIsANamedConstructorAndIsNotReported'     | null | []
        'AStaticMethodAssigningAPrivateStaticFieldIsNotReported'                         | null | []
        'ACompoundAssignmentCountsAsAWrite'                                              | null | []
        'AnIncrementCountsAsAWrite'                                                      | null | []
        'ADecrementCountsAsAWrite'                                                       | null | []
        'AStaticAccessorOverAPrivateStaticFieldIsReportedAReadIsNotAWrite'               | null | [4]
        'AMutatingCallOnAPrivateStaticFieldIsReportedPutCannotBeToldFromSize'            | null | [4]
        'WritingANonPrivateStaticFieldIsNotAnException'                                  | null | [4]
        'WritingAPrivateInstanceFieldThroughAReceiverIsNotWritingStaticState'            | null | [4]
        'WritingALocalVariableThatShadowsTheFieldIsNotWritingTheField'                   | null | [4]
        'AStaticNativeMethodHasNoBodyAndWritesNothing'                                   | null | [2]
        'ASealedUtilityClassIsExempt'                                                    | null | []
        'APublicClassWithAnImplicitConstructorIsNotAUtilityClass'                        | null | [2]
        'APackagePrivateClassWithAnImplicitConstructorIsAUtilityClass'                   | null | []
        'AProtectedConstructorDisqualifiesTheType'                                       | null | [4]
        'OneInstanceMethodDisqualifiesTheType'                                           | null | [4]
        'AnInstanceFieldDoesNotDisqualifyAUtilityClass'                                  | null | []
        'AnInterfaceWithOnlyStaticMethodsIsExemptItHasNoConstructor'                     | null | []
        'AnInterfaceWithADefaultMethodIsNotExempt'                                       | null | [2]
        'AnEnumWithNoInstanceMethodsIsExemptItsImplicitConstructorIsPrivate'             | null | []
        'NestedTypesAreEvaluatedIndependently'                                           | null | []
        'AMainMethodIsNotReported'                                                       | null | []
        'AMethodNamedMainWithADifferentSignatureIsReported'                              | null | [2]
        'AJUnitLifecycleMethodIsNotReportedJUnitRequiresItToBeStatic'                    | null | []
        'AFullyQualifiedLifecycleAnnotationIsRecognisedToo'                              | null | []
        'AMethodSourceProviderIsReportedNothingDistinguishesItFromAnyOtherStatic'        | null | [2]
        'AnUnrelatedAnnotationDoesNotExemptAStaticHelper'                                | null | [3]
        'AStaticFactoryReturningTheEnclosingTypeIsNotReported'                           | null | []
        'AStaticFactoryReturningADirectlyImplementedInterfaceIsNotReported'              | null | []
        'AStaticFactoryReturningTheSecondOfTwoDeclaredInterfacesIsNotReported'           | null | []
        'AStaticFactoryInAnInterfaceMayReturnTheInterfaceTheInterfaceExtends'            | null | []
        'AStaticFactoryReturningAnUnrelatedTypeIsReported'                               | null | [2]
        'AStaticFactoryReturningASuperclassIsReportedOnlyDirectlyDeclaredTypesAreExempt' | null | [2]
        'AStaticFactoryReturningATransitivelyInheritedInterfaceIsReported'               | null | [2]
        'AVoidStaticMethodIsReportedVoidNamesNoType'                                     | null | [2]
        'APrimitiveReturnIsReportedAPrimitiveNamesNoType'                                | null | [2]
        'AnArrayOfTheEnclosingTypeIsReportedAnArrayIsNotTheConstructor'                  | null | [2]
        'AGenericFactoryReturningTheEnclosingTypeIsNotReported'                          | null | []
        'ALombokUtilityClassIsExemptEvenThoughItsMethodsAreNotDeclaredStatic'            | null | []
        'TheUtilityClassMarkerIsMatchedBySimpleNameFromAnyPackage'                       | null | []
        'WithoutTheMarkerTheSamePublicClassIsReportedTheMarkerIsWhatExemptsIt'           | null | [2]
        'AnUnrelatedAnnotationOnTheEnclosingTypeDoesNotExemptItsStaticMethods'           | null | [3]
        'TheMarkerOnAnEnclosingTypeDoesNotExemptANestedTypeSStaticMethods'               | null | [6]
        'SeveralStaticHelpersInOneClassAreEachReported'                                  | null | [4, 5]
    }
}
